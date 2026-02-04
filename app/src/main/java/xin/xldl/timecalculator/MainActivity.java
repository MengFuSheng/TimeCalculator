package xin.xldl.timecalculator;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

public class MainActivity extends AppCompatActivity {

    // 用于记录用户设置的时间制式（可以在SharedPreferences中持久化）
    private boolean is24HourFormat = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. 初始化ThreeTenABP时间库
        initializeTimeLibrary();

        // 2. 设置卡片点击监听器
        setupCardListeners();

        // 3. 可选：从SharedPreferences加载用户偏好设置
        loadUserPreferences();

        // 4. 显示欢迎信息
        showWelcomeMessage();
    }

    /**
     * 初始化时间处理库
     */
    private void initializeTimeLibrary() {
        try {
            // 初始化ThreeTenABP库，确保时间计算功能可用
            AndroidThreeTen.init(this);

            // 测试库是否正常工作
            LocalDateTime now = LocalDateTime.now();
            String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            System.out.println("时间库初始化成功，当前时间：" + formattedTime);

        } catch (Exception e) {
            // 如果初始化失败，显示错误信息但仍允许应用运行
            Toast.makeText(this, "时间库初始化失败，部分功能可能受限", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    /**
     * 设置功能卡片点击事件
     */
    private void setupCardListeners() {
        // 时间间隔计算卡片
        CardView cardInterval = findViewById(R.id.card_interval);
        cardInterval.setOnClickListener(v -> navigateToTimeIntervalActivity());

        // 添加点击效果
        cardInterval.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.7f);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1.0f);
                    break;
            }
            return false;
        });

        // 时间点推算卡片
        CardView cardPoint = findViewById(R.id.card_point);
        cardPoint.setOnClickListener(v -> navigateToTimePointActivity());

        // 添加点击效果
        cardPoint.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.7f);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1.0f);
                    break;
            }
            return false;
        });

        // 时间单位换算卡片
        CardView cardConverter = findViewById(R.id.card_converter);
        cardConverter.setOnClickListener(v -> navigateToTimeConverterActivity());

        // 添加点击效果
        cardConverter.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.setAlpha(0.7f);
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.setAlpha(1.0f);
                    break;
            }
            return false;
        });

        // 长按显示功能介绍
        cardInterval.setOnLongClickListener(v -> {
            Toast.makeText(MainActivity.this,
                    "计算任意两个时间点之间的精确时长",
                    Toast.LENGTH_SHORT).show();
            return true;
        });

        cardPoint.setOnLongClickListener(v -> {
            Toast.makeText(MainActivity.this,
                    "根据基准时间推算未来或过去的时间点",
                    Toast.LENGTH_SHORT).show();
            return true;
        });

        cardConverter.setOnLongClickListener(v -> {
            Toast.makeText(MainActivity.this,
                    "在不同时间单位之间进行快速转换",
                    Toast.LENGTH_SHORT).show();
            return true;
        });
    }

    /**
     * 导航到时间间隔计算界面
     */
    private void navigateToTimeIntervalActivity() {
        Intent intent = new Intent(MainActivity.this, TimeIntervalActivity.class);
        // 传递时间制式偏好
        intent.putExtra("IS_24_HOUR_FORMAT", is24HourFormat);
        startActivity(intent);

        // 添加Activity切换动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 导航到时间点推算界面
     */
    private void navigateToTimePointActivity() {
        Intent intent = new Intent(MainActivity.this, TimePointActivity.class);
        // 传递时间制式偏好
        intent.putExtra("IS_24_HOUR_FORMAT", is24HourFormat);
        startActivity(intent);

        // 添加Activity切换动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 导航到时间单位换算界面
     */
    private void navigateToTimeConverterActivity() {
        Intent intent = new Intent(MainActivity.this, TimeConverterActivity.class);
        startActivity(intent);

        // 添加Activity切换动画
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 加载用户偏好设置
     */
    private void loadUserPreferences() {
        // 使用SharedPreferences加载用户的时间制式偏好
        android.content.SharedPreferences prefs = getSharedPreferences("TimeCalculatorPrefs", MODE_PRIVATE);
        is24HourFormat = prefs.getBoolean("time_format_24h", true); // 默认为24小时制

        // 也可以加载其他设置，如主题颜色等
    }

    /**
     * 保存用户偏好设置
     */
    private void saveUserPreferences() {
        android.content.SharedPreferences prefs = getSharedPreferences("TimeCalculatorPrefs", MODE_PRIVATE);
        android.content.SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("time_format_24h", is24HourFormat);
        editor.apply();
    }

    /**
     * 显示欢迎信息
     */
    private void showWelcomeMessage() {
        // 首次启动时显示欢迎信息
        android.content.SharedPreferences prefs = getSharedPreferences("TimeCalculatorPrefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("first_launch", true);

        if (isFirstLaunch) {
            // 显示简短的欢迎提示
            Toast.makeText(this, "欢迎使用时间计算器！", Toast.LENGTH_SHORT).show();

            // 标记已不是首次启动
            android.content.SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("first_launch", false);
            editor.apply();
        }
    }

    /**
     * 创建选项菜单
     */
    @Override
    public boolean onCreateOptionsMenu(android.view.Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    /**
     * 处理选项菜单选择
     */
    @Override
    public boolean onOptionsItemSelected(android.view.MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            // 打开设置界面
            showSettingsDialog();
            return true;
        } else if (id == R.id.action_about) {
            // 显示关于对话框
            showAboutDialog();
            return true;
        } else if (id == R.id.action_help) {
            // 显示帮助信息
            showHelpDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * 显示设置对话框
     */
    private void showSettingsDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("设置");

        // 创建布局
        View view = getLayoutInflater().inflate(R.layout.dialog_settings, null);

        // 设置时间制式选择
        android.widget.Switch switchTimeFormat = view.findViewById(R.id.switch_time_format);
        switchTimeFormat.setChecked(is24HourFormat);
        switchTimeFormat.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                is24HourFormat = isChecked;
                saveUserPreferences();
                Toast.makeText(MainActivity.this,
                        isChecked ? "已切换为24小时制" : "已切换为12小时制",
                        Toast.LENGTH_SHORT).show();
            }
        });

        builder.setView(view);
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    /**
     * 显示关于对话框
     */
    private void showAboutDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("关于时间计算器");
        builder.setMessage("版本：1.0\n\n" +
                "功能：\n" +
                "• 时间间隔计算\n" +
                "• 时间点推算\n" +
                "• 时间单位换算\n\n" +
                "技术支持：ThreeTenABP 时间库\n" +
                "界面设计：Material Design");
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    /**
     * 显示帮助对话框
     */
    private void showHelpDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("使用帮助");
        builder.setMessage("1. 时间间隔计算：选择开始和结束时间，计算精确时长\n\n" +
                "2. 时间点推算：输入基准时间和时长，推算未来或过去时间\n\n" +
                "3. 时间单位换算：在不同时间单位间快速转换\n\n" +
                "提示：\n" +
                "• 可在设置中切换12/24小时制\n" +
                "• 长按功能卡片查看简要说明");
        builder.setPositiveButton("确定", null);
        builder.show();
    }

    /**
     * 处理返回键按下
     */
    @Override
    public void onBackPressed() {
        // 双击返回键退出应用
        if (isDoubleBackPressedToExit()) {
            super.onBackPressed();
        }
    }

    private long backPressedTime = 0;

    /**
     * 双击返回键退出应用
     */
    private boolean isDoubleBackPressedToExit() {
        if (System.currentTimeMillis() - backPressedTime < 2000) {
            return true;
        } else {
            Toast.makeText(this, "再次点击返回键退出应用", Toast.LENGTH_SHORT).show();
            backPressedTime = System.currentTimeMillis();
            return false;
        }
    }

    /**
     * 应用生命周期管理
     */
    @Override
    protected void onResume() {
        super.onResume();
        // 每次返回主界面时刷新用户偏好设置
        loadUserPreferences();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // 保存当前设置
        saveUserPreferences();
    }
}