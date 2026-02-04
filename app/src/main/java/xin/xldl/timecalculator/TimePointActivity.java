package xin.xldl.timecalculator;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.threeten.bp.Duration;
import org.threeten.bp.LocalDateTime;
import org.threeten.bp.format.DateTimeFormatter;

import java.text.DecimalFormat;
import java.util.Locale;

public class TimePointActivity extends AppCompatActivity {

    // 时间相关变量
    private LocalDateTime baseDateTime = null;
    private LocalDateTime resultDateTime = null;
    private boolean is24HourFormat = true;
    private boolean isAddOperation = true; // true=增加, false=减少

    // 界面控件
    private RadioGroup rgTimeFormat, rgOperation;
    private RadioButton rb24h, rb12h, rbAdd, rbSubtract;
    private TextView tvBaseDate, tvBaseTime, tvResultTime, tvDetailedResult;
    private EditText etDurationValue;
    private Spinner spinnerUnit;
    private CardView cardBaseTime, cardResult;

    // 格式化器
    private DateTimeFormatter dateFormatter;
    private DateTimeFormatter timeFormatter;
    private DateTimeFormatter fullFormatter;
    private DateTimeFormatter resultFormatter;

    // 常量
    private static final String PREFS_NAME = "TimeCalculatorPrefs";
    private static final String PREF_TIME_FORMAT = "time_format_24h";
    private static final String PREF_BASE_TIME = "last_base_time";
    private static final String PREF_DURATION = "last_duration";
    private static final String PREF_UNIT = "last_unit";
    private static final String PREF_OPERATION = "last_operation";

    // 单位换算（以秒为基准）
    private static final double[] UNIT_MULTIPLIERS = {
            1,          // 秒
            60,         // 分钟
            3600,       // 小时
            86400,      // 天
            604800,     // 周
            2592000,    // 月（30天）
            31536000    // 年（365天）
    };

    private static final String[] UNIT_NAMES = {
            "秒", "分钟", "小时", "天", "周", "月", "年"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d("TimePointActivity", "=== TimePointActivity 启动 ===");

        try {
            // 设置布局
            setContentView(R.layout.activity_time_point);
            Log.d("TimePointActivity", "布局文件加载完成: activity_time_point.xml");

            // 检查布局文件是否正确加载
            View rootView = findViewById(android.R.id.content);
            if (rootView == null) {
                Log.e("TimePointActivity", "根布局为空，布局文件可能有问题");
                showErrorAndExit("布局文件加载失败");
                return;
            }

            // 初始化时间库（简化版本，避免ThreeTenABP问题）
            initializeTimeLibrary();

            // 初始化视图
            initViews();

            // 设置格式化器
            setupFormatters();

            // 设置Spinner
            setupSpinner();

            // 加载保存的设置
            loadSavedData();

            // 更新显示
            updateDisplay();

            Log.d("TimePointActivity", "=== TimePointActivity 初始化完成 ===");

        } catch (Exception e) {
            Log.e("TimePointActivity", "初始化过程发生异常", e);
            e.printStackTrace();
            showErrorAndExit(e.getMessage());
        }
    }

    /**
     * 初始化ThreeTenABP时间库
     */
    private void initializeTimeLibrary() {
        try {
            com.jakewharton.threetenabp.AndroidThreeTen.init(this);
        } catch (Exception e) {
            Toast.makeText(this, "时间库初始化失败", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    /**
     * 显示时间库错误对话框
     */
    private void showLibraryErrorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("初始化错误")
                .setMessage("时间处理库初始化失败，无法使用此功能。\n\n可能原因：\n1. 应用权限不足\n2. 系统时间设置异常\n3. 应用文件损坏\n\n建议：\n1. 重启应用\n2. 检查系统时间设置")
                .setPositiveButton("确定", (dialog, which) -> finish())
                .setCancelable(false)
                .show();
    }

    /**
     * 初始化所有界面控件
     */
    private void initViews() {
        Log.d("TimePointActivity", "开始初始化视图");

        // 标题栏按钮
        ImageButton btnBack = findViewById(R.id.btn_back);
        ImageButton btnHelp = findViewById(R.id.btn_help);

        if (btnBack == null) Log.e("TimePointActivity", "btn_back 未找到");
        if (btnHelp == null) Log.e("TimePointActivity", "btn_help 未找到");

        btnBack.setOnClickListener(v -> onBackPressed());
        btnHelp.setOnClickListener(v -> showHelpDialog());

        // 时间制式选择
        rgTimeFormat = findViewById(R.id.rg_time_format);
        rb24h = findViewById(R.id.rb_24h);
        rb12h = findViewById(R.id.rb_12h);

        if (rgTimeFormat == null) Log.e("TimePointActivity", "rg_time_format 未找到");
        if (rb24h == null) Log.e("TimePointActivity", "rb_24h 未找到");
        if (rb12h == null) Log.e("TimePointActivity", "rb_12h 未找到");

        // 操作选择
        rgOperation = findViewById(R.id.rg_operation);
        rbAdd = findViewById(R.id.rb_add);
        rbSubtract = findViewById(R.id.rb_subtract);

        if (rgOperation == null) Log.e("TimePointActivity", "rg_operation 未找到");
        if (rbAdd == null) Log.e("TimePointActivity", "rb_add 未找到");
        if (rbSubtract == null) Log.e("TimePointActivity", "rb_subtract 未找到");

        // 基准时间显示
        tvBaseDate = findViewById(R.id.tv_base_date);
        tvBaseTime = findViewById(R.id.tv_base_time);

        if (tvBaseDate == null) Log.e("TimePointActivity", "tv_base_date 未找到");
        if (tvBaseTime == null) Log.e("TimePointActivity", "tv_base_time 未找到");

        // 时间输入 - 这是关键控件！
        etDurationValue = findViewById(R.id.et_duration_value);
        if (etDurationValue == null) {
            Log.e("TimePointActivity", "et_duration_value 未找到 - 这是关键问题！");
        } else {
            Log.d("TimePointActivity", "et_duration_value 找到成功");
        }

        // Spinner 单位选择 - 这是导致闪退的控件！
        spinnerUnit = findViewById(R.id.spinner_unit);
        if (spinnerUnit == null) {
            Log.e("TimePointActivity", "spinner_unit 未找到 - 这就是空指针的原因！");
            // 临时创建Spinner避免崩溃
            spinnerUnit = new Spinner(this);
            String[] tempUnits = {"秒", "分钟", "小时", "天", "周"};
            ArrayAdapter<String> tempAdapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_spinner_item, tempUnits);
            tempAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerUnit.setAdapter(tempAdapter);
            Toast.makeText(this, "Spinner初始化失败，使用临时替代", Toast.LENGTH_LONG).show();
        } else {
            Log.d("TimePointActivity", "spinner_unit 找到成功");
        }

        // 结果显示
        tvResultTime = findViewById(R.id.tv_result_time);
        tvDetailedResult = findViewById(R.id.tv_detailed_result);

        if (tvResultTime == null) Log.e("TimePointActivity", "tv_result_time 未找到");
        if (tvDetailedResult == null) Log.e("TimePointActivity", "tv_detailed_result 未找到");

        // 卡片
        cardBaseTime = findViewById(R.id.card_base_time);
        cardResult = findViewById(R.id.card_result);

        if (cardBaseTime == null) Log.e("TimePointActivity", "card_base_time 未找到");
        if (cardResult == null) Log.e("TimePointActivity", "card_result 未找到");

        Log.d("TimePointActivity", "视图初始化完成");

        // 设置监听器 - 添加空检查
        try {
            setupListeners();
            Log.d("TimePointActivity", "监听器设置完成");
        } catch (Exception e) {
            Log.e("TimePointActivity", "设置监听器失败: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 显示错误并退出
     */
    private void showErrorAndExit(String errorMessage) {
        runOnUiThread(() -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("初始化错误")
                    .setMessage("时间点推算功能初始化失败：\n\n" + errorMessage +
                            "\n\n可能原因：\n1. 布局文件损坏\n2. 控件ID不匹配\n3. 资源文件错误\n\n将返回主菜单。")
                    .setPositiveButton("确定", (dialog, which) -> {
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .setCancelable(false)
                    .show();
        });
    }

    /**
     * 设置Spinner适配器
     */
    private void setupSpinner() {
        if (spinnerUnit == null) {
            Log.e("TimePointActivity", "setupSpinner: spinnerUnit 为空");
            return;
        }

        try {
            String[] units = UNIT_NAMES;
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    units
            );

            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerUnit.setAdapter(adapter);

            Log.d("TimePointActivity", "Spinner适配器设置完成，项目数: " + units.length);

        } catch (Exception e) {
            Log.e("TimePointActivity", "设置Spinner适配器失败: " + e.getMessage());

            // 使用简单备选方案
            String[] fallbackUnits = {"秒", "分钟", "小时", "天", "周"};
            ArrayAdapter<String> fallbackAdapter = new ArrayAdapter<>(
                    this,
                    android.R.layout.simple_spinner_item,
                    fallbackUnits
            );
            spinnerUnit.setAdapter(fallbackAdapter);
        }
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
            resultFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss", Locale.CHINA);
        } else {
            timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a", Locale.ENGLISH);
            fullFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 hh:mm:ss a", Locale.ENGLISH);
            resultFormatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日 hh:mm:ss a", Locale.ENGLISH);
        }
    }

    /**
     * 设置所有按钮监听器
     */
    private void setupListeners() {
        Log.d("TimePointActivity", "开始设置监听器");

        // 时间制式切换 - 添加空检查
        if (rgTimeFormat != null) {
            rgTimeFormat.setOnCheckedChangeListener((group, checkedId) -> {
                is24HourFormat = checkedId == R.id.rb_24h;
                updateFormatters();
                updateDisplay();
                saveTimeFormatPreference();
            });
        }

        // 操作选择 - 添加空检查
        if (rgOperation != null) {
            rgOperation.setOnCheckedChangeListener((group, checkedId) -> {
                isAddOperation = checkedId == R.id.rb_add;
                saveOperationPreference();
                calculateResult();
            });
        }

        // 基准时间按钮
        View btnSelectBaseDate = findViewById(R.id.btn_select_base_date);
        View btnSelectBaseTime = findViewById(R.id.btn_select_base_time);

        if (btnSelectBaseDate != null) {
            btnSelectBaseDate.setOnClickListener(v -> showDatePicker());
        }
        if (btnSelectBaseTime != null) {
            btnSelectBaseTime.setOnClickListener(v -> {
                if (baseDateTime == null) {
                    Toast.makeText(this, "请先选择基准日期", Toast.LENGTH_SHORT).show();
                    showDatePicker();
                } else {
                    showTimePicker();
                }
            });
        }

        // 快捷设置按钮
        View btnSetBaseNow = findViewById(R.id.btn_set_base_now);
        View btnClearBase = findViewById(R.id.btn_clear_base);
        View btnCopyBase = findViewById(R.id.btn_copy_base);

        if (btnSetBaseNow != null) btnSetBaseNow.setOnClickListener(v -> setCurrentTime());
        if (btnClearBase != null) btnClearBase.setOnClickListener(v -> clearBaseTime());
        if (btnCopyBase != null) btnCopyBase.setOnClickListener(v -> copyBaseTime());

        // 计算按钮
        View btnCalculate = findViewById(R.id.btn_calculate);
        if (btnCalculate != null) {
            btnCalculate.setOnClickListener(v -> calculateResult());
        }

        // 结果操作按钮
        View btnCopyResult = findViewById(R.id.btn_copy_result);
        View btnSetAsBase = findViewById(R.id.btn_set_as_base);

        if (btnCopyResult != null) btnCopyResult.setOnClickListener(v -> copyResult());
        if (btnSetAsBase != null) btnSetAsBase.setOnClickListener(v -> setResultAsBase());

        // 时长输入监听 - 添加空检查
        if (etDurationValue != null) {
            etDurationValue.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    saveDurationPreference();
                    calculateResult();
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
        } else {
            Log.e("TimePointActivity", "etDurationValue 为空，无法设置TextWatcher");
        }

        // 单位选择监听 - 这是导致闪退的地方，必须添加空检查！
        if (spinnerUnit != null) {
            spinnerUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    saveUnitPreference();
                    calculateResult();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // 什么都不做
                }
            });
            Log.d("TimePointActivity", "Spinner监听器设置成功");
        } else {
            Log.e("TimePointActivity", "spinnerUnit 为空，无法设置监听器");
            // 显示错误提示
            Toast.makeText(this, "单位选择器初始化失败，部分功能可能受限", Toast.LENGTH_LONG).show();
        }

        // 设置预设按钮监听器
        setupPresetButtons();

        Log.d("TimePointActivity", "所有监听器设置完成");
    }

    /**
     * 设置预设按钮监听器
     */
    private void setupPresetButtons() {
        findViewById(R.id.btn_preset_30m).setOnClickListener(v -> setPresetDuration(30, 1)); // 30分钟
        findViewById(R.id.btn_preset_1h).setOnClickListener(v -> setPresetDuration(1, 2));   // 1小时
        findViewById(R.id.btn_preset_1d).setOnClickListener(v -> setPresetDuration(1, 3));   // 1天
        findViewById(R.id.btn_preset_1w).setOnClickListener(v -> setPresetDuration(1, 4));   // 1周
        findViewById(R.id.btn_preset_1m).setOnClickListener(v -> setPresetDuration(1, 5));   // 1个月
        findViewById(R.id.btn_preset_1y).setOnClickListener(v -> setPresetDuration(1, 6));   // 1年
    }

    /**
     * 设置预设时长
     */
    private void setPresetDuration(double value, int unitIndex) {
        etDurationValue.setText(String.valueOf((int)value));
        spinnerUnit.setSelection(unitIndex);

        Toast.makeText(this,
                String.format("已设置为: %.0f %s", value, UNIT_NAMES[unitIndex]),
                Toast.LENGTH_SHORT).show();
    }

    /**
     * 显示日期选择器
     */
    private void showDatePicker() {
        LocalDateTime currentDateTime = baseDateTime != null ? baseDateTime : LocalDateTime.now();

        int year = currentDateTime.getYear();
        int month = currentDateTime.getMonthValue() - 1;
        int day = currentDateTime.getDayOfMonth();

        DatePickerDialog datePicker = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    // 创建或更新基准时间
                    if (baseDateTime == null) {
                        baseDateTime = LocalDateTime.of(selectedYear, selectedMonth + 1, selectedDay, 0, 0, 0);
                    } else {
                        baseDateTime = LocalDateTime.of(
                                selectedYear,
                                selectedMonth + 1,
                                selectedDay,
                                baseDateTime.getHour(),
                                baseDateTime.getMinute(),
                                baseDateTime.getSecond()
                        );
                    }

                    updateDisplay();
                    saveBaseTime();

                    // 自动显示时间选择器
                    showTimePicker();
                },
                year, month, day
        );

        datePicker.setTitle("选择基准日期");
        datePicker.show();
    }

    /**
     * 显示时间选择器
     */
    private void showTimePicker() {
        if (baseDateTime == null) return;

        int hour = baseDateTime.getHour();
        int minute = baseDateTime.getMinute();

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
                        showAmPmDialog(selectedHour, selectedMinute);
                    } else {
                        // 直接更新24小时制时间
                        updateBaseTime(selectedHour, selectedMinute, 0);
                    }
                },
                displayHour,
                minute,
                is24HourFormat
        );

        timePicker.setTitle("选择基准时间");
        timePicker.show();
    }

    /**
     * 显示上午/下午选择对话框
     */
    private void showAmPmDialog(int hour12, int minute) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择上午/下午");

        final String[] amPmOptions = {"上午 (AM)", "下午 (PM)"};
        final int[] selectedIndex = {0};

        builder.setSingleChoiceItems(amPmOptions, 0, (dialog, which) -> {
            selectedIndex[0] = which;
        });

        builder.setPositiveButton("确定", (dialog, which) -> {
            int hour24 = convert12to24(hour12, selectedIndex[0] == 1);
            updateBaseTime(hour24, minute, 0);
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
                return 12;
            } else {
                return hour12 + 12;
            }
        } else {
            if (hour12 == 12) {
                return 0;
            } else {
                return hour12;
            }
        }
    }

    /**
     * 更新基准时间
     */
    private void updateBaseTime(int hour, int minute, int second) {
        if (baseDateTime != null) {
            baseDateTime = LocalDateTime.of(
                    baseDateTime.getYear(),
                    baseDateTime.getMonthValue(),
                    baseDateTime.getDayOfMonth(),
                    hour, minute, second
            );

            updateDisplay();
            saveBaseTime();
        }
    }

    /**
     * 设置当前时间为基准时间
     */
    private void setCurrentTime() {
        baseDateTime = LocalDateTime.now();
        updateDisplay();
        saveBaseTime();

        Toast.makeText(this, "基准时间已设为当前时间", Toast.LENGTH_SHORT).show();
    }

    /**
     * 清除基准时间
     */
    private void clearBaseTime() {
        baseDateTime = null;
        resultDateTime = null;
        updateDisplay();
        saveBaseTime();

        Toast.makeText(this, "基准时间已清空", Toast.LENGTH_SHORT).show();
    }

    /**
     * 复制基准时间
     */
    private void copyBaseTime() {
        if (baseDateTime != null) {
            String formattedTime = formatDateTime(baseDateTime);
            copyToClipboard(formattedTime);
            Toast.makeText(this, "基准时间已复制到剪贴板", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先设置基准时间", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制计算结果
     */
    private void copyResult() {
        if (resultDateTime != null) {
            String formattedTime = formatDateTime(resultDateTime);
            copyToClipboard(formattedTime);
            Toast.makeText(this, "推算结果已复制到剪贴板", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先计算推算结果", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 将结果设为新的基准时间
     */
    private void setResultAsBase() {
        if (resultDateTime != null) {
            baseDateTime = resultDateTime;
            updateDisplay();
            saveBaseTime();

            Toast.makeText(this, "推算结果已设为新的基准时间", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "请先计算推算结果", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制文本到剪贴板
     */
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("时间计算结果", text);
        clipboard.setPrimaryClip(clip);
    }

    /**
     * 更新显示
     */
    private void updateDisplay() {
        // 更新基准时间显示
        if (baseDateTime != null) {
            tvBaseDate.setText(baseDateTime.format(dateFormatter));
            String timeText = baseDateTime.format(timeFormatter);
            if (!is24HourFormat) {
                timeText = timeText.replace("AM", "上午").replace("PM", "下午");
            }
            tvBaseTime.setText(timeText);
            cardBaseTime.setCardBackgroundColor(Color.WHITE);
        } else {
            tvBaseDate.setText("未选择");
            tvBaseTime.setText("未选择");
            cardBaseTime.setCardBackgroundColor(0xFFF5F5F5);
        }

        // 更新卡片状态
        if (baseDateTime != null && etDurationValue.getText().toString().trim().length() > 0) {
            cardResult.setCardBackgroundColor(Color.WHITE);
        } else {
            cardResult.setCardBackgroundColor(0xFFF5F5F5);
        }
    }

    /**
     * 计算推算结果
     */
    private void calculateResult() {
        // 检查必要条件
        if (baseDateTime == null) {
            tvResultTime.setText("请先设置基准时间");
            tvResultTime.setTextColor(Color.parseColor("#757575"));
            tvDetailedResult.setVisibility(View.GONE);
            return;
        }

        String durationStr = etDurationValue.getText().toString().trim();
        if (durationStr.isEmpty()) {
            tvResultTime.setText("请输入时间长度");
            tvResultTime.setTextColor(Color.parseColor("#757575"));
            tvDetailedResult.setVisibility(View.GONE);
            return;
        }

        try {
            // 解析输入值
            double durationValue = Double.parseDouble(durationStr);
            int unitIndex = spinnerUnit.getSelectedItemPosition();

            // 转换为秒
            double totalSeconds = durationValue * UNIT_MULTIPLIERS[unitIndex];

            // 计算新时间点
            if (isAddOperation) {
                resultDateTime = baseDateTime.plusSeconds((long) totalSeconds);
            } else {
                resultDateTime = baseDateTime.minusSeconds((long) totalSeconds);
            }

            // 格式化结果
            String formattedResult = formatDateTime(resultDateTime);

            // 显示结果
            tvResultTime.setText(formattedResult);
            tvResultTime.setTextColor(Color.parseColor("#4CAF50"));

            // 显示详细结果
            showDetailedResult(durationValue, unitIndex, totalSeconds);
            tvDetailedResult.setVisibility(View.VISIBLE);

        } catch (NumberFormatException e) {
            tvResultTime.setText("请输入有效的数值");
            tvResultTime.setTextColor(Color.parseColor("#F44336"));
            tvDetailedResult.setVisibility(View.GONE);
        } catch (Exception e) {
            tvResultTime.setText("计算错误：" + e.getMessage());
            tvResultTime.setTextColor(Color.parseColor("#F44336"));
            tvDetailedResult.setVisibility(View.GONE);
        }
    }

    /**
     * 显示详细结果
     */
    private void showDetailedResult(double durationValue, int unitIndex, double totalSeconds) {
        DecimalFormat df = new DecimalFormat("#.###");

        String operationText = isAddOperation ? "增加" : "减少";
        String unitName = UNIT_NAMES[unitIndex];

        StringBuilder detailedText = new StringBuilder();
        detailedText.append("推算详情：\n\n");
        detailedText.append("基准时间：").append(formatDateTime(baseDateTime)).append("\n");
        detailedText.append("操作类型：").append(operationText).append("\n");
        detailedText.append("时间长度：").append(df.format(durationValue)).append(" ").append(unitName).append("\n");
        detailedText.append("换算秒数：").append(df.format(totalSeconds)).append(" 秒\n\n");

        // 显示其他单位表示
        detailedText.append("其他单位表示：\n");
        for (int i = 0; i < UNIT_NAMES.length; i++) {
            if (i != unitIndex) {
                double otherUnitValue = totalSeconds / UNIT_MULTIPLIERS[i];
                if (otherUnitValue >= 0.001) { // 只显示有意义的值
                    detailedText.append("• ").append(df.format(otherUnitValue))
                            .append(" ").append(UNIT_NAMES[i]).append("\n");
                }
            }
        }

        // 显示相对时间
        LocalDateTime now = LocalDateTime.now();
        Duration fromNow = Duration.between(now, resultDateTime);
        long daysFromNow = fromNow.toDays();

        detailedText.append("\n相对于现在：\n");
        if (daysFromNow > 0) {
            detailedText.append("• 未来 ").append(daysFromNow).append(" 天后");
        } else if (daysFromNow < 0) {
            detailedText.append("• 过去 ").append(Math.abs(daysFromNow)).append(" 天前");
        } else {
            detailedText.append("• 就是今天");
        }

        tvDetailedResult.setText(detailedText.toString());
    }

    /**
     * 格式化日期时间显示
     */
    private String formatDateTime(LocalDateTime dateTime) {
        String formatted = dateTime.format(resultFormatter);
        if (!is24HourFormat) {
            formatted = formatted.replace("AM", "上午").replace("PM", "下午");
        }
        return formatted;
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
     * 保存基准时间
     */
    private void saveBaseTime() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        if (baseDateTime != null) {
            editor.putString(PREF_BASE_TIME, baseDateTime.toString());
        } else {
            editor.remove(PREF_BASE_TIME);
        }

        editor.apply();
    }

    /**
     * 保存时长偏好
     */
    private void saveDurationPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_DURATION, etDurationValue.getText().toString());
        editor.apply();
    }

    /**
     * 保存单位偏好
     */
    private void saveUnitPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_UNIT, spinnerUnit.getSelectedItemPosition());
        editor.apply();
    }

    /**
     * 保存操作偏好
     */
    private void saveOperationPreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean(PREF_OPERATION, isAddOperation);
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

        // 加载操作偏好
        isAddOperation = prefs.getBoolean(PREF_OPERATION, true);
        if (isAddOperation) {
            rbAdd.setChecked(true);
        } else {
            rbSubtract.setChecked(true);
        }

        // 加载基准时间
        String baseTimeStr = prefs.getString(PREF_BASE_TIME, null);
        if (baseTimeStr != null) {
            try {
                baseDateTime = LocalDateTime.parse(baseTimeStr);
            } catch (Exception e) {
                // 解析失败，忽略
            }
        }

        // 加载时长
        String durationStr = prefs.getString(PREF_DURATION, "");
        etDurationValue.setText(durationStr);

        // 加载单位
        int unitIndex = prefs.getInt(PREF_UNIT, 2); // 默认小时
        if (unitIndex >= 0 && unitIndex < UNIT_NAMES.length) {
            spinnerUnit.setSelection(unitIndex);
        }
    }

    /**
     * 显示帮助对话框
     */
    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("时间点推算帮助");

        String helpText = "使用方法：\n\n" +
                "1. 设置基准时间：\n" +
                "   • 点击日期和时间按钮选择\n" +
                "   • 可使用'设为现在'快速设置\n" +
                "   • 可以复制或清空基准时间\n\n" +
                "2. 设置时间增减：\n" +
                "   • 选择增加或减少操作\n" +
                "   • 输入时间长度数值\n" +
                "   • 选择时间单位（秒、分、时、天、周、月、年）\n" +
                "   • 可使用常用时长预设\n\n" +
                "3. 查看结果：\n" +
                "   • 点击'开始推算'按钮\n" +
                "   • 结果可以复制到剪贴板\n" +
                "   • 可以设为新的基准时间\n\n" +
                "4. 注意事项：\n" +
                "   • 月按30天计算，年按365天计算\n" +
                "   • 支持小数输入（如1.5小时）\n" +
                "   • 自动保存最近使用的设置";

        builder.setMessage(helpText);
        builder.setPositiveButton("确定", null);
        builder.setNeutralButton("查看示例", (dialog, which) -> showExample());

        builder.show();
    }

    /**
     * 显示示例
     */
    private void showExample() {
        // 设置示例基准时间
        baseDateTime = LocalDateTime.of(2023, 10, 1, 9, 0, 0);

        // 设置示例参数
        etDurationValue.setText("2");
        spinnerUnit.setSelection(2); // 小时
        rbAdd.setChecked(true);

        updateDisplay();
        calculateResult();

        Toast.makeText(this, "已加载示例：今天9:00 + 2小时", Toast.LENGTH_SHORT).show();
    }

    /**
     * 处理返回键
     */
    @Override
    public void onBackPressed() {
        // 保存数据
        saveTimeFormatPreference();
        saveBaseTime();
        saveDurationPreference();
        saveUnitPreference();
        saveOperationPreference();

        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 保存状态
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveTimeFormatPreference();
        saveBaseTime();
        saveDurationPreference();
        saveUnitPreference();
        saveOperationPreference();
    }
}