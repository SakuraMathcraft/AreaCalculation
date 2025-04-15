package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Locale;
import android.content.Intent;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

public class StatsActivity extends AppCompatActivity {

    private TextView statsText;
    private Button clearBtn;
    private Button exportBtn;
    private LinearLayout exportListLayout;
    private File historyFile;

    private final ActivityResultLauncher<Intent> exportLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Uri uri = result.getData().getData();
                    try {
                        OutputStream out = getContentResolver().openOutputStream(uri);
                        byte[] data = readFileBytes(historyFile);
                        if (out != null && data != null) {
                            out.write(data);
                            out.close();
                            Toast.makeText(this, "✅ 已成功导出轨迹", Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Toast.makeText(this, "❌ 导出失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        statsText = findViewById(R.id.statsText);
        clearBtn = findViewById(R.id.clearBtn);
        exportBtn = findViewById(R.id.exportBtn);
        exportListLayout = findViewById(R.id.exportListLayout);
        historyFile = new File(getExternalFilesDir(null), "history.json");

        exportBtn.setOnClickListener(v -> {
            if (!historyFile.exists()) {
                Toast.makeText(this, "⚠️ 无历史记录可导出", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "exported_track_" +
                    new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new java.util.Date()) + ".json");

            exportLauncher.launch(intent);
        });

        clearBtn.setOnClickListener(v -> {
            if (historyFile.exists() && historyFile.delete()) {
                statsText.setText("暂无历史数据");
                exportListLayout.removeAllViews();
                Toast.makeText(this, "✅ 历史记录已清除", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ 无历史记录可清除", Toast.LENGTH_SHORT).show();
            }
        });

        loadStatistics();
    }

    private void loadStatistics() {
        if (!historyFile.exists()) {
            statsText.setText("暂无历史数据");
            return;
        }

        try {
            FileInputStream in = new FileInputStream(historyFile);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();

            String json = out.toString("UTF-8");
            JSONArray array = new JSONArray(json);

            double totalArea = 0;
            double totalDistance = 0;
            int totalSteps = 0;
            int totalRealSteps = 0;

            StringBuilder sb = new StringBuilder();
            sb.append("📊 历史轨迹统计\n\n");

            exportListLayout.removeAllViews();

            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                double area = obj.optDouble("area", 0);
                double dist = obj.optDouble("distance", 0);
                int steps = obj.optInt("steps", 0);
                int realSteps = obj.optInt("realSteps", -1);
                String time = obj.optString("time", "未知时间");

                totalArea += area;
                totalDistance += dist;
                totalSteps += steps;
                if (realSteps >= 0) totalRealSteps += realSteps;

                // 展示每条记录
                sb.append(String.format(Locale.getDefault(),
                        "📍 记录时间：%s\n🗺️ 面积：%.2f㎡\n📏 周长：%.2fm\n🚶 步长数：%d\n👣 真实步数：%s\n\n",
                        time, area, dist, steps, realSteps >= 0 ? realSteps : "暂无"));

                // 导出按钮
                Button itemBtn = new Button(this);
                itemBtn.setText("📤 导出第 " + (i + 1) + " 条轨迹");
                int finalI = i;
                itemBtn.setOnClickListener(v -> exportTrack(array, finalI));
                exportListLayout.addView(itemBtn);
            }

            sb.insert(0, String.format(Locale.getDefault(),
                    "📁 总记录数：%d\n🚶 总步长数：%d\n👣 总真实步数：%d\n📏 总距离：%.2f 米\n🗺️ 总面积：%.2f 平方米\n\n",
                    array.length(), totalSteps, totalRealSteps, totalDistance, totalArea));

            statsText.setText(sb.toString());

        } catch (Exception e) {
            Toast.makeText(this, "❌ 数据读取失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportTrack(JSONArray array, int index) {
        try {
            JSONObject record = array.getJSONObject(index);
            JSONArray path = record.optJSONArray("path");

            if (path == null || path.length() == 0) {
                Toast.makeText(this, "⚠️ 当前记录无轨迹数据", Toast.LENGTH_SHORT).show();
                return;
            }

            // 启动文件选择器，允许用户选择文件保存路径
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/json");
            intent.putExtra(Intent.EXTRA_TITLE, "export_track_" + (index + 1) + ".json");

            exportLauncher.launch(intent);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "❌ 导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private byte[] readFileBytes(File file) {
        try {
            FileInputStream fis = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                bos.write(buffer, 0, len);
            }
            fis.close();
            return bos.toByteArray();
        } catch (IOException e) {
            return null;
        }
    }
}
