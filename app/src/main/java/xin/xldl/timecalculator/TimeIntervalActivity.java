package xin.xldl.timecalculator;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.jakewharton.threetenabp.AndroidThreeTen;

import org.threeten.bp.*;
import org.threeten.bp.format.DateTimeFormatter;
import java.util.Locale;

public class TimeIntervalActivity extends AppCompatActivity {

    // 时间相关变量
    private LocalDateTime startDateTime = null;
    private LocalDateTime endDateTime = null;
    private boolean is24HourFormat = true;

    // 界面控件
    private RadioGroup rgTimeFormat;
    private RadioButton rb24h, rb12h;
    private TextView tvStartDate, tvStartTime, tvEndDate, tvEndTime;
    private TextView tvTimeInterval, tvDetailedResult;
    private CardView cardStartTime, cardEndTime, cardResult;

    // 日期时间格式化器
    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter fullFormatter;

    // 常量
    private static final String PREFS_NAME = "TimeCalculatorPrefs";
    private static final String PREF_TIME_FORMAT = "time_format_24h";
    private static final String PREF_START_TIME = "last_start_time";
    private static final String PREF_END_TIME = "last_end_time";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_interval);

        // 初始化时间库
        initializeTimeLibrary();

        // 初始化视图
        initViews();

        // 设置格式化器
        setupFormatters();

        // 加载保存的设置和时间
        loadSavedData();

        // 更新显示
        updateTimeDisplay();
    }

    /**
     * 初始化ThreeTenABP时间库
     */
    private void initializeTimeLibrary() {
        try {
            AndroidThreeTen.init(this);
        } catch (Exception e) {
            Toast.makeText(this, "时间库初始化失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 初始化所有界面控件
     */
    private void initViews() {
        // 标题栏按钮
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnHelp = findViewById(R.id.btn_help);

        btnBack.setOnClickListener(v -> onBackPressed());
        btnHelp.setOnClickListener(v -> showHelpDialog());

        // 时间制式选择
        rgTimeFormat = findViewById(R.id.rg_time_format);
        rb24h = findViewById(R.id.rb_24h);
        rb12h = findViewById(R.id.rb_12h);

        // 开始时间相关
        tvStartDate = findViewById(R.id.tv_start_date);
        tvStartTime = findViewById(R.id.tv_start_time);

        // 结束时间相关
        tvEndDate = findViewById(R.id.tv_end_date);
        tvEndTime = findViewById(R.id.tv_end_time);

        // 结果相关
        tvTimeInterval = findViewById(R.id.tv_time_interval);
        tvDetailedResult = findViewById(R.id.tv_detailed_result);

        // 卡片
        cardStartTime = findViewById(R.id.card_start_time);
        cardEndTime = findViewById(R.id.card_end_time);
        cardResult = findViewById(R.id.card_result);

        // 设置监听器
        setupListeners();
    }

    /**
     * 设置格式化器
     */
    private void setupFormatters() {
        dateFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日", Locale.CHINA);
        updateFormatters();
    }

    /**
     * 根据当前制式更新格式化器
     */
    private void updateFormatters() {
        if (is24HourFormat) {
            timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss", Locale.CHINA);
            fullFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA);
        } else {
            timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);
            fullFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 hh:mm:ss a", Locale.ENGLISH);
        }
    }

    /**
     * 设置所有按钮监听器
     */
    private void setupListeners() {
        // 时间制式切换
        rgTimeFormat.setOnCheckedChangeListener((group, checkedId) -> {
            is24HourFormat = checkedId == R.id.rb_24h;
            updateFormatters();
            updateTimeDisplay();
            saveTimeFormatPreference();
        });

        // 开始时间按钮
        findViewById(R.id.btn_select_start_date).setOnClickListener(v -> showDatePicker(true));
        findViewById(R.id.btn_select_start_time).setOnClickListener(v -> {
            if (startDateTime == null) {
                Toast.makeText(this, "请先选择开始日期", Toast.LENGTH_SHORT).show();
                showDatePicker(true);
            } else {
                showTimePicker(true);
            }
        });

        // 结束时间按钮
        findViewById(R.id.btn_select_end_date).setOnClickListener(v -> showDatePicker(false));
        findViewById(R.id.btn_select_end_time).setOnClickListener(v -> {
            if (endDateTime == null) {
                Toast.makeText(this, "请先选择结束日期", Toast.LENGTH_SHORT).show();
                showDatePicker(false);
            } else {
                showTimePicker(false);
            }
        });

        // 快捷设置按钮
        findViewById(R.id.btn_set_start_now).setOnClickListener(v -> setCurrentTime(true));
        findViewById(R.id.btn_set_end_now).setOnClickListener(v -> setCurrentTime(false));

        findViewById(R.id.btn_clear_start).setOnClickListener(v -> clearTime(true));
        findViewById(R.id.btn_clear_end).setOnClickListener(v -> clearTime(false));

        // 计算按钮
        findViewById(R.id.btn_calculate).setOnClickListener(v -> calculateInterval());

        // 交换按钮
        findViewById(R.id.btn_swap_times).setOnClickListener(v -> swapTimes());

        // 卡片点击效果
        setupCardClickListeners();
    }

    /**
     * 设置卡片点击效果
     */
    private void setupCardClickListeners() {
        cardStartTime.setOnClickListener(v -> {
            if (startDateTime == null) {
                showDatePicker(true);
            }
        });

        cardEndTime.setOnClickListener(v -> {
            if (endDateTime == null) {
                showDatePicker(false);
            }
        });
    }

    /**
     * 显示日期选择器
     */
    private void showDatePicker(boolean isStartTime) {
        LocalDateTime currentDateTime = null;

        // 如果当前没有时间，使用当前时间
        if (currentDateTime == null) {
            currentDateTime = LocalDateTime.now();
        } else {
            currentDateTime = isStartTime ? startDateTime : endDateTime;
        }

        int year = currentDateTime.getYear();
        int month = currentDateTime.getMonthValue() - 1; // 月份从0开始
        int day = currentDateTime.getDayOfMonth();

        LocalDateTime finalCurrentDateTime = currentDateTime;
        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // 创建新的LocalDateTime
                    LocalDateTime newDateTime = LocalDateTime.of(
                            selectedYear,
                            selectedMonth + 1,
                            selectedDay,
                            finalCurrentDateTime.getHour(),
                            finalCurrentDateTime.getMinute(),
                            finalCurrentDateTime.getSecond()
                    );

                    // 保存选择
                    if (isStartTime) {
                        startDateTime = newDateTime;
                    } else {
                        endDateTime = newDateTime;
                    }

                    updateTimeDisplay();
                    saveTimeData();

                    // 自动显示时间选择器
                    showTimePicker(isStartTime);
                },
                year, month, day
        );

        datePicker.setTitle(isStartTime ? "选择开始日期" : "选择结束日期");
        datePicker.show();
    }

    /**
     * 显示时间选择器
     */
    private void showTimePicker(boolean isStartTime) {
        LocalDateTime currentDateTime = isStartTime ? startDateTime : endDateTime;

        if (currentDateTime == null) return;

        int hour = currentDateTime.getHour();
        int minute = currentDateTime.getMinute();

        // 处理12小时制显示
        int displayHour = hour;
        if (!is24HourFormat) {
            displayHour = hour % 12;
            if (displayHour == 0) {
                displayHour = 12;
            }
        }

        TimePickerDialog timePicker = new TimePickerDialog(
                this,
                (view, selectedHour, selectedMinute) -> {
                    // 处理12小时制下的时间选择
                    if (!is24HourFormat) {
                        showAmPmDialog(isStartTime, selectedHour, selectedMinute);
                    } else {
                        // 直接更新24小时制时间
                        updateSelectedTime(isStartTime, selectedHour, selectedMinute, 0);
                    }
                },
                displayHour,
                minute,
                is24HourFormat
        );

        timePicker.setTitle(isStartTime ? "选择开始时间" : "选择结束时间");
        timePicker.show();
    }

    /**
     * 显示上午/下午选择对话框（用于12小时制）
     */
    private void showAmPmDialog(boolean isStartTime, int hour12, int minute) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择上午/下午");

        final String[] amPmOptions = {"上午 (AM)", "下午 (PM)"};
        final int[] selectedIndex = {0}; // 默认选择上午

        builder.setSingleChoiceItems(amPmOptions, 0, (dialog, which) -> {
            selectedIndex[0] = which;
        });

        builder.setPositiveButton("确定", (dialog, which) -> {
            int hour24 = convert12to24(hour12, selectedIndex[0] == 1);
            updateSelectedTime(isStartTime, hour24, minute, 0);
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 将12小时制时间转换为24小时制
     */
    private int convert12to24(int hour12, boolean isPm) {
        if (isPm) {
            if (hour12 == 12) {
                return 12; // 下午12点就是12点
            } else {
                return hour12 + 12;
            }
        } else {
            if (hour12 == 12) {
                return 0; // 上午12点就是0点
            } else {
                return hour12;
            }
        }
    }

    /**
     * 更新选择的时间
     */
    private void updateSelectedTime(boolean isStartTime, int hour, int minute, int second) {
        LocalDateTime currentDateTime = isStartTime ? startDateTime : endDateTime;

        if (currentDateTime != null) {
            LocalDateTime newDateTime = LocalDateTime.of(
                    currentDateTime.getYear(),
                    currentDateTime.getMonthValue(),
                    currentDateTime.getDayOfMonth(),
                    hour, minute, second
            );

            if (isStartTime) {
                startDateTime = newDateTime;
            } else {
                endDateTime = newDateTime;
            }

            updateTimeDisplay();
            saveTimeData();
        }
    }

    /**
     * 设置当前时间
     */
    private void setCurrentTime(boolean isStartTime) {
        LocalDateTime now = LocalDateTime.now();

        if (isStartTime) {
            startDateTime = now;
        } else {
            endDateTime = now;
        }

        updateTimeDisplay();
        saveTimeData();

        Toast.makeText(this,
                (isStartTime ? "开始时间" : "结束时间") + "已设为当前时间",
                Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除时间
     */
    private void clearTime(boolean isStartTime) {
        if (isStartTime) {
            startDateTime = null;
            tvStartDate.setText("未选择");
            tvStartTime.setText("未选择");
        } else {
            endDateTime = null;
            tvEndDate.setText("未选择");
            tvEndTime.setText("未选择");
        }

        saveTimeData();
        updateResultDisplay();
    }

    /**
     * 更新时间显示
     */
    private void updateTimeDisplay() {
        // 更新开始时间显示
        if (startDateTime != null) {
            tvStartDate.setText(startDateTime.format(dateFormatter));
            String timeText = startDateTime.format(timeFormatter);
            if (!is24HourFormat) {
                timeText = timeText.replace("AM", "上午").replace("PM", "下午");
            }
            tvStartTime.setText(timeText);
        }

        // 更新结束时间显示
        if (endDateTime != null) {
            tvEndDate.setText(endDateTime.format(dateFormatter));
            String timeText = endDateTime.format(timeFormatter);
            if (!is24HourFormat) {
                timeText = timeText.replace("AM", "上午").replace("PM", "下午");
            }
            tvEndTime.setText(timeText);
        }

        // 更新卡片状态
        updateCardStates();

        // 更新结果（如果有两个时间）
        if (startDateTime != null && endDateTime != null) {
            calculateInterval();
        }
    }

    /**
     * 更新卡片状态
     */
    private void updateCardStates() {
        // 开始时间卡片
        if (startDateTime != null) {
            cardStartTime.setCardBackgroundColor(Color.WHITE);
        } else {
            cardStartTime.setCardBackgroundColor(0xFFF5F5F5);
        }

        // 结束时间卡片
        if (endDateTime != null) {
            cardEndTime.setCardBackgroundColor(Color.WHITE);
        } else {
            cardEndTime.setCardBackgroundColor(0xFFF5F5F5);
        }

        // 结果卡片
        if (startDateTime != null && endDateTime != null) {
            cardResult.setCardBackgroundColor(Color.WHITE);
        } else {
            cardResult.setCardBackgroundColor(0xFFF5F5F5);
        }
    }

    /**
     * 交换开始和结束时间
     */
    private void swapTimes() {
        if (startDateTime == null && endDateTime == null) {
            Toast.makeText(this, "请先选择时间", Toast.LENGTH_SHORT).show();
            return;
        }

        LocalDateTime temp = startDateTime;
        startDateTime = endDateTime;
        endDateTime = temp;

        updateTimeDisplay();
        saveTimeData();

        Toast.makeText(this, "开始和结束时间已交换", Toast.LENGTH_SHORT).show();
    }

    /**
     * 计算时间间隔
     */
    private void calculateInterval() {
        if (startDateTime == null || endDateTime == null) {
            tvTimeInterval.setText("请选择开始时间和结束时间");
            tvTimeInterval.setTextColor(Color.parseColor("#757575"));
            tvDetailedResult.setVisibility(View.GONE);
            return;
        }

        try {
            // 计算时间差
            Duration duration = Duration.between(startDateTime, endDateTime);
            long totalSeconds = duration.getSeconds();
            long totalMillis = duration.toMillis();

            boolean isNegative = totalSeconds < 0;
            if (isNegative) {
                duration = duration.abs();
                totalSeconds = Math.abs(totalSeconds);
                totalMillis = Math.abs(totalMillis);
            }

            // 计算各个时间单位
            long days = duration.toDays();
            long hours = duration.toHours() % 24;
            long minutes = duration.toMinutes() % 60;
            long seconds = totalSeconds % 60;
            long millis = totalMillis % 1000;

            // 构建结果显示
            String resultText;
            String detailedText;

            if (days > 0) {
                resultText = String.format("时间间隔：%d天 %d小时 %d分钟", days, hours, minutes);
            } else if (hours > 0) {
                resultText = String.format("时间间隔：%d小时 %d分钟 %d秒", hours, minutes, seconds);
            } else if (minutes > 0) {
                resultText = String.format("时间间隔：%d分钟 %d秒", minutes, seconds);
            } else {
                resultText = String.format("时间间隔：%d秒 %d毫秒", seconds, millis);
            }

            // 添加符号提示
            if (isNegative) {
                resultText += "（结束时间早于开始时间）";
                tvTimeInterval.setTextColor(Color.parseColor("#F44336"));
            } else {
                tvTimeInterval.setTextColor(Color.parseColor("#4CAF50"));
            }

            // 构建详细结果
            detailedText = String.format(
                    "详细结果：\n" +
                            "• 总天数：%d天\n" +
                            "• 总小时数：%d小时\n" +
                            "• 总分钟数：%d分钟\n" +
                            "• 总秒数：%d秒\n" +
                            "• 总毫秒数：%d毫秒\n\n" +
                            "开始时间：%s\n" +
                            "结束时间：%s",
                    days,
                    duration.toHours(),
                    duration.toMinutes(),
                    totalSeconds,
                    totalMillis,
                    formatDateTime(startDateTime),
                    formatDateTime(endDateTime)
            );

            // 显示结果
            tvTimeInterval.setText(resultText);
            tvDetailedResult.setText(detailedText);
            tvDetailedResult.setVisibility(View.VISIBLE);

        } catch (Exception e) {
            tvTimeInterval.setText("计算错误：" + e.getMessage());
            tvTimeInterval.setTextColor(Color.parseColor("#F44336"));
            tvDetailedResult.setVisibility(View.GONE);
        }
    }

    /**
     * 格式化日期时间显示
     */
    private String formatDateTime(LocalDateTime dateTime) {
        String formatted = dateTime.format(fullFormatter);
        if (!is24HourFormat) {
            formatted = formatted.replace("AM", "上午").replace("PM", "下午");
        }
        return formatted;
    }

    /**
     * 更新结果显示
     */
    private void updateResultDisplay() {
        if (startDateTime == null || endDateTime == null) {
            tvTimeInterval.setText("请选择开始时间和结束时间");
            tvTimeInterval.setTextColor(Color.parseColor("#757575"));
            tvDetailedResult.setVisibility(View.GONE);
        }
    }

    /**
     * 保存时间格式偏好
     */
    private void saveTimeFormatPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_TIME_FORMAT, is24HourFormat);
        editor.apply();
    }

    /**
     * 保存时间数据
     */
    private void saveTimeData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (startDateTime != null) {
            editor.putString(PREF_START_TIME, startDateTime.toString());
        } else {
            editor.remove(PREF_START_TIME);
        }

        if (endDateTime != null) {
            editor.putString(PREF_END_TIME, endDateTime.toString());
        } else {
            editor.remove(PREF_END_TIME);
        }

        editor.apply();
    }

    /**
     * 加载保存的数据
     */
    private void loadSavedData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 加载时间格式偏好
        is24HourFormat = prefs.getBoolean(PREF_TIME_FORMAT, true);
        if (is24HourFormat) {
            rb24h.setChecked(true);
        } else {
            rb12h.setChecked(true);
        }

        // 加载保存的时间
        String startTimeStr = prefs.getString(PREF_START_TIME, null);
        String endTimeStr = prefs.getString(PREF_END_TIME, null);

        if (startTimeStr != null) {
            try {
                startDateTime = LocalDateTime.parse(startTimeStr);
            } catch (Exception e) {
                // 解析失败，忽略
            }
        }

        if (endTimeStr != null) {
            try {
                endDateTime = LocalDateTime.parse(endTimeStr);
            } catch (Exception e) {
                // 解析失败，忽略
            }
        }
    }

    /**
     * 显示帮助对话框
     */
    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("时间间隔计算帮助");

        String helpText = "使用方法：\n\n" +
                "1. 选择时间制式：\n" +
                "   • 24小时制：直接显示24小时时间\n" +
                "   • 12小时制：需要额外选择上午/下午\n\n" +
                "2. 设置开始和结束时间：\n" +
                "   • 点击日期和时间按钮分别选择\n" +
                "   • 可使用'设为现在'快速设置\n" +
                "   • 使用'清空'按钮清除选择\n\n" +
                "3. 计算时间间隔：\n" +
                "   • 点击'计算时间间隔'按钮\n" +
                "   • 可交换开始和结束时间\n\n" +
                "4. 注意事项：\n" +
                "   • 支持计算过去和未来的时间间隔\n" +
                "   • 结果显示精确到毫秒\n" +
                "   • 自动保存最近使用的时间";

        builder.setMessage(helpText);
        builder.setPositiveButton("确定", null);
        builder.setNeutralButton("查看示例", (dialog, which) -> showExample());

        builder.show();
    }

    /**
     * 显示示例
     */
    private void showExample() {
        // 设置示例时间
        startDateTime = LocalDateTime.of(2023, 10, 1, 9, 30, 0);
        endDateTime = LocalDateTime.of(2023, 10, 2, 14, 45, 30);

        updateTimeDisplay();
        calculateInterval();

        Toast.makeText(this, "已加载示例时间", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理返回键
     */
    @Override
    public void onBackPressed() {
        // 保存数据
        saveTimeData();
        saveTimeFormatPreference();

        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 保存状态
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveTimeData();
        saveTimeFormatPreference();
    }
}