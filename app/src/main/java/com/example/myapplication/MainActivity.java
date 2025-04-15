package com.example.myapplication;
import android.util.Log;
import android.os.Build;
import android.view.Window;
import android.view.WindowManager;
import android.Manifest;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.amap.api.location.AMapLocationClient;
import com.amap.api.location.AMapLocationClientOption;
import com.amap.api.maps.*;
import com.amap.api.maps.model.*;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.PowerManager;
import android.net.Uri;

public class MainActivity extends AppCompatActivity {

    private MapView mapView;
    private AMap aMap;
    private AMapLocationClient locationClient;
    private AMapLocationClientOption locationOption;
    private SensorManager sensorManager;
    private int baseStepCount = -1;
    private int realSteps = 0;
    private List<LatLng> pointList = new ArrayList<>();
    private LatLng lastLatLng = null;
    private float minDistance = 2.0f;
    private int stepCounter = 0;

    private EditText stepInput;
    private TextView areaTextView, timeTextView, stepCountTextView;
    private boolean isDarkTheme = false;
    private long startTimeMillis, endTimeMillis;

    // Step Counter SensorListener
    private final SensorEventListener stepListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER) {
                int total = (int) event.values[0];
                if (baseStepCount == -1) {
                    baseStepCount = total;  // 初始化基准值
                    Log.d("StepCounter", "Base step count initialized: " + baseStepCount);
                }
                realSteps = total - baseStepCount;
                runOnUiThread(() -> stepCountTextView.setText("真实步数：" + realSteps)); // Update UI in main thread
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {}
    };

    // Permissions logic to request location
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (Boolean.TRUE.equals(result.get(Manifest.permission.ACCESS_FINE_LOCATION))) {
                    startLocationUpdates();
                } else {
                    showMessage("⚠️ 未授予定位权限");
                }
            });

    private void requestLocationPermission() {
        permissionLauncher.launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACTIVITY_RECOGNITION // ✅ 添加步数传感器权限
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        MapsInitializer.updatePrivacyShow(this, true, true);
        MapsInitializer.updatePrivacyAgree(this, true);
        super.onCreate(savedInstanceState);

        // Immersive Status Bar: Ensure map stretches into the status bar area
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(android.graphics.Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_main);

        // Initialize sensor manager and register for step counter sensor
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            Toast.makeText(this, "⚠️ 当前设备不支持步数传感器", Toast.LENGTH_SHORT).show();
        }

        // Initialize map view and other UI elements
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        aMap = mapView.getMap();

        stepInput = findViewById(R.id.stepInput);
        areaTextView = findViewById(R.id.areaResult);
        timeTextView = findViewById(R.id.timeResult);
        stepCountTextView = findViewById(R.id.stepCountText);

        // Location and Map settings
        MyLocationStyle myLocationStyle = new MyLocationStyle();
        myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE); // Blue dot rotates with movement
        myLocationStyle.showMyLocation(true); // Ensure the blue dot is visible
        myLocationStyle.strokeWidth(0); // No border for the blue dot
        myLocationStyle.radiusFillColor(0x00000000); // Remove the blue dot's radius background
        aMap.setMyLocationStyle(myLocationStyle); // Set map location style
        aMap.setMyLocationEnabled(true); // Enable the location blue dot

        // Bottom sheet behavior
        View bottomSheet = findViewById(R.id.cardView);
        BottomSheetBehavior<View> bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setPeekHeight(960);
        bottomSheetBehavior.setHideable(false);

        // Start button click listener
        findViewById(R.id.startBtn).setOnClickListener(v -> {
            if (!isLocationEnabled()) {
                showMessage("⚠️ 请开启定位服务后重试");
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                return;
            }
            try {
                String input = stepInput.getText().toString();
                minDistance = Float.parseFloat(input);
            } catch (Exception e) {
                minDistance = 2.0f;
                showMessage("⚠️ 无效步长，默认2米");
            }
            stepCounter = 0;
            startTimeMillis = System.currentTimeMillis();

            // ✅ 启动前台服务
            Intent serviceIntent = new Intent(this, TrackingService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

            // ✅ 申请定位+识别权限
            requestLocationPermission();

            // ✅ 提示加入电池白名单（避免被系统后台限制）
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && pm != null
                    && !pm.isIgnoringBatteryOptimizations(packageName)) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + packageName));
                startActivity(intent);
            }
        });

        // Stop button click listener
        findViewById(R.id.stopBtn).setOnClickListener(v -> {
            if (startTimeMillis == 0) {
                showMessage("⚠️ 请先点击“开始记录坐标(GPS)”再结束记录");
                return;
            }
            endTimeMillis = System.currentTimeMillis();
            stopLocationUpdates();
            drawClosedPolygon();

            double area = calculateArea(pointList);
            double perimeter = calculatePerimeter(pointList);
            String duration = getDuration();

            areaTextView.setText(String.format("面积：%.2f㎡\n周长：%.2fm", area, perimeter));
            timeTextView.setText(String.format("开始：%s\n结束：%s\n用时：%s",
                    formatTime(startTimeMillis), formatTime(endTimeMillis), duration));
            stepCountTextView.setText("步数：" + stepCounter);

            TextView realStepView = findViewById(R.id.realStepCountText);
            realStepView.setText("真实步数：" + realSteps);

            saveToHistory(area, perimeter, stepCounter);

            if (pointList.size() >= 3) {
                double gap = distanceBetween(pointList.get(0), pointList.get(pointList.size() - 1));
                if (gap > 10) showMessage("⚠️ 路径可能未闭合（起终点向量差距大）");
            }

            showMessage("✅ 轨迹记录结束");
            stopService(new Intent(this, TrackingService.class));
        });

        // Zoom button click listener
        findViewById(R.id.zoomBtn).setOnClickListener(v -> {
            if (lastLatLng != null) {
                aMap.animateCamera(CameraUpdateFactory.newLatLngZoom(lastLatLng, 18));
                showMessage("✅ 已定位到当前位置");
            } else {
                showMessage("⚠️ 尚未获取定位，请先点击“开始记录坐标(GPS)”并允许使用确切的位置信息");
            }
        });

        // Layer button click listener
        findViewById(R.id.layerBtn).setOnClickListener(v -> {
            boolean isSatellite = aMap.getMapType() == AMap.MAP_TYPE_SATELLITE;
            aMap.setMapType(isSatellite ? AMap.MAP_TYPE_NORMAL : AMap.MAP_TYPE_SATELLITE);
            showMessage("✅ 已切换为" + (isSatellite ? "交通图" : "卫星图"));
        });

        // Theme button click listener
        findViewById(R.id.themeBtn).setOnClickListener(v -> {
            isDarkTheme = !isDarkTheme;
            aMap.setMapType(isDarkTheme ? AMap.MAP_TYPE_NIGHT : AMap.MAP_TYPE_NORMAL);
            showMessage("✅ 已切换为" + (isDarkTheme ? "夜间模式" : "白天模式"));
        });

        // Replay button click listener
        findViewById(R.id.replayBtn).setOnClickListener(v -> {
            if (pointList.size() < 2) {
                showMessage("⚠️ 轨迹太短，无法回放");
                return;
            }
            aMap.clear();
            new Thread(() -> {
                for (int i = 1; i < pointList.size(); i++) {
                    final List<LatLng> subList = pointList.subList(0, i + 1);
                    runOnUiThread(() -> aMap.addPolyline(new PolylineOptions()
                            .addAll(subList)
                            .width(6)
                            .color(0xFF00BCD4)));
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ignored) {
                    }
                }
                runOnUiThread(() -> showMessage("✅ 开始轨迹回放"));
            }).start();
        });

        // Stats button click listener
        findViewById(R.id.statsBtn).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, StatsActivity.class));
        });
    }

    private void saveToHistory(double area, double perimeter, int steps) {
        try {
            File file = new File(getExternalFilesDir(null), "history.json");
            JSONArray all = new JSONArray();

            // 读取已有内容
            if (file.exists()) {
                FileInputStream in = new FileInputStream(file);
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                in.close();
                String existing = out.toString("UTF-8");
                if (!existing.isEmpty()) {
                    all = new JSONArray(existing);
                }
            }

            // 构建新记录
            JSONObject record = new JSONObject();
            record.put("area", area);
            record.put("distance", perimeter);
            record.put("steps", steps);
            record.put("time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date()));

            // 添加轨迹 path
            JSONArray pathArray = new JSONArray();
            for (LatLng point : pointList) {
                JSONObject p = new JSONObject();
                p.put("lat", point.latitude);
                p.put("lng", point.longitude);
                pathArray.put(p);
            }
            record.put("path", pathArray);

            // 加入到全部记录中
            all.put(record);

            // 写入文件
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(all.toString().getBytes("UTF-8"));
            fos.close();

            showMessage("✅ 记录已保存，共 " + all.length() + " 条");

        } catch (Exception e) {
            showMessage("⚠️ 保存失败：" + e.getMessage());
        }
    }

    private boolean isLocationEnabled() {
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
        return lm != null && (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER));
    }

    private void startLocationUpdates() {
        pointList.clear();
        lastLatLng = null;
        aMap.clear();

        try {
            locationClient = new AMapLocationClient(getApplicationContext());
        } catch (Exception e) {
            showMessage("⚠️ 定位初始化失败：" + e.getMessage());
            return;
        }

        locationOption = new AMapLocationClientOption();
        locationOption.setLocationMode(AMapLocationClientOption.AMapLocationMode.Hight_Accuracy); // 高精度定位
        locationOption.setInterval(1000); // 定位间隔1秒
        locationClient.setLocationOption(locationOption);

        locationClient.setLocationListener(location -> {
            if (location != null && location.getErrorCode() == 0) {
                LatLng current = new LatLng(location.getLatitude(), location.getLongitude());
                if (lastLatLng == null || distanceBetween(lastLatLng, current) >= minDistance) {
                    pointList.add(current);
                    lastLatLng = current;
                    stepCounter++;
                    runOnUiThread(() -> aMap.addPolyline(new PolylineOptions()
                            .addAll(pointList)
                            .width(6)
                            .color(0xFF1E90FF)));  // 在地图上添加轨迹
                }
            }
        });

        locationClient.startLocation();
        showMessage("✅ 开始记录轨迹");
    }

    private void stopLocationUpdates() {
        if (locationClient != null) {
            locationClient.stopLocation();
            locationClient.onDestroy();
        }
    }

    private void drawClosedPolygon() {
        if (pointList.size() >= 3) {
            if (!pointList.get(0).equals(pointList.get(pointList.size() - 1))) {
                pointList.add(pointList.get(0)); // 闭合
            }
            aMap.addPolygon(new PolygonOptions()
                    .addAll(pointList)
                    .fillColor(0x3300FF00)
                    .strokeWidth(4)
                    .strokeColor(0xFF00AA00));
        }
    }

    private double calculateArea(List<LatLng> list) {
        if (list.size() < 3) return 0;
        double area = 0;
        for (int i = 0; i < list.size(); i++) {
            LatLng p1 = list.get(i);
            LatLng p2 = list.get((i + 1) % list.size());
            area += (p1.longitude * p2.latitude - p2.longitude * p1.latitude);
        }
        return Math.abs(area / 2.0) * 111139 * 111139; // 平面近似
    }

    private double calculatePerimeter(List<LatLng> list) {
        double total = 0;
        for (int i = 1; i < list.size(); i++) {
            total += distanceBetween(list.get(i - 1), list.get(i));
        }
        if (list.size() > 2) {
            total += distanceBetween(list.get(0), list.get(list.size() - 1));
        }
        return total;
    }

    private double distanceBetween(LatLng p1, LatLng p2) {
        float[] result = new float[1];
        Location.distanceBetween(p1.latitude, p1.longitude, p2.latitude, p2.longitude, result);
        return result[0];
    }

    private String formatTime(long millis) {
        return new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date(millis));
    }

    private String getDuration() {
        long seconds = (endTimeMillis - startTimeMillis) / 1000;
        return (seconds / 60) + "分" + (seconds % 60) + "秒";
    }

    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();

        // 补充：确保步数监听器在前台时重新注册
        Sensor stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        if (stepSensor != null) {
            sensorManager.registerListener(stepListener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(stepListener);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }
}
