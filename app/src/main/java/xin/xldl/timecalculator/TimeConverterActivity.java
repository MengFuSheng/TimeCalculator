package xin.xldl.timecalculator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TimeConverterActivity extends AppCompatActivity {

    // 时间单位定义
    private static final String[] TIME_UNITS = {
            "纳秒", "微秒", "毫秒", "秒", "分钟", "小时", "天", "周", "月", "年"
    };

    // 单位换算因子（以秒为基准）
    private static final double[] UNIT_FACTORS = {
            1_000_000_000.0,  // 纳秒 -> 秒 (1秒 = 1,000,000,000纳秒)
            1_000_000.0,      // 微秒 -> 秒
            1_000.0,          // 毫秒 -> 秒
            1.0,              // 秒 -> 秒
            1.0 / 60.0,       // 分钟 -> 秒 (1分钟 = 60秒)
            1.0 / 3600.0,     // 小时 -> 秒 (1小时 = 3600秒)
            1.0 / 86400.0,    // 天 -> 秒 (1天 = 86400秒)
            1.0 / 604800.0,   // 周 -> 秒 (1周 = 604800秒)
            1.0 / 2592000.0,  // 月 -> 秒 (1月 = 30天 = 2,592,000秒)
            1.0 / 31536000.0  // 年 -> 秒 (1年 = 365天 = 31,536,000秒)
    };

    // 特殊单位定义
    private static final double WORKDAY_HOURS = 8.0; // 工作日按8小时计算
    private static final double LEAP_YEAR_DAYS = 366.0; // 闰年天数

    // 界面控件
    private EditText etInputValue;
    private TextView tvOutputValue;
    private TextView tvMainResult;
    private Spinner spinnerFromUnit, spinnerToUnit;
    private LinearLayout layoutResultsGrid;
    private ListView listHistory;
    private TextView tvNoHistory;
    private CardView cardResults;

    // 当前设置
    private double inputValue = 1.0;
    private int fromUnitIndex = 7; // 默认：周
    private int toUnitIndex = 3;   // 默认：天
    private int precision = 2;     // 显示精度：0=低, 1=中, 2=高

    // 历史记录
    private List<HistoryItem> historyList = new ArrayList<>();
    private static final int MAX_HISTORY = 20;

    // 历史记录项类
    private static class HistoryItem {
        String fromValue;
        String fromUnit;
        String toValue;
        String toUnit;
        String timestamp;

        HistoryItem(String fromValue, String fromUnit, String toValue, String toUnit) {
            this.fromValue = fromValue;
            this.fromUnit = fromUnit;
            this.toValue = toValue;
            this.toUnit = toUnit;
            this.timestamp = new SimpleDateFormat("HH:mm:ss", Locale.CHINA).format(new Date());
        }

        String getDisplayText() {
            return fromValue + " " + fromUnit + " = " + toValue + " " + toUnit;
        }
    }

    // 常量
    private static final String PREFS_NAME = "TimeConverterPrefs";
    private static final String PREF_INPUT_VALUE = "input_value";
    private static final String PREF_FROM_UNIT = "from_unit";
    private static final String PREF_TO_UNIT = "to_unit";
    private static final String PREF_PRECISION = "precision";
    private static final String PREF_HISTORY = "conversion_history";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_time_converter);

        // 初始化视图
        initViews();

        // 设置Spinner
        setupSpinners();

        // 设置监听器
        setupListeners();

        // 加载保存的设置
        loadSavedData();

        // 加载历史记录
        loadHistory();

        // 初始计算
        calculateConversion();
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

        // 输入输出控件
        etInputValue = findViewById(R.id.et_input_value);
        tvOutputValue = findViewById(R.id.tv_output_value);
        tvMainResult = findViewById(R.id.tv_main_result);

        // Spinner
        spinnerFromUnit = findViewById(R.id.spinner_from_unit);
        spinnerToUnit = findViewById(R.id.spinner_to_unit);

        // 结果网格
        layoutResultsGrid = findViewById(R.id.layout_results_grid);

        // 历史记录
        listHistory = findViewById(R.id.list_history);
        tvNoHistory = findViewById(R.id.tv_no_history);

        // 卡片
        cardResults = findViewById(R.id.card_results);
    }

    /**
     * 设置Spinner
     */
    private void setupSpinners() {
        // 创建适配器
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TIME_UNITS
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // 设置适配器
        spinnerFromUnit.setAdapter(adapter);
        spinnerToUnit.setAdapter(adapter);
    }

    /**
     * 设置所有按钮监听器
     */
    private void setupListeners() {
        // 输入值变化监听
        etInputValue.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (s.toString().trim().isEmpty()) {
                    inputValue = 0;
                } else {
                    try {
                        inputValue = Double.parseDouble(s.toString().trim());
                    } catch (NumberFormatException e) {
                        inputValue = 0;
                    }
                }
                saveInputValue();
                calculateConversion();
            }
        });

        // 单位选择监听
        spinnerFromUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                fromUnitIndex = position;
                saveFromUnit();
                calculateConversion();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        spinnerToUnit.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                toUnitIndex = position;
                saveToUnit();
                calculateConversion();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // 交换按钮
        findViewById(R.id.btn_swap_units).setOnClickListener(v -> swapUnits());

        // 操作按钮
        findViewById(R.id.btn_copy_result).setOnClickListener(v -> copyResult());
        findViewById(R.id.btn_clear_input).setOnClickListener(v -> clearInput());
        findViewById(R.id.btn_set_one).setOnClickListener(v -> setInputToOne());

        // 精度按钮
        findViewById(R.id.btn_precision_low).setOnClickListener(v -> setPrecision(0));
        findViewById(R.id.btn_precision_medium).setOnClickListener(v -> setPrecision(1));
        findViewById(R.id.btn_precision_high).setOnClickListener(v -> setPrecision(2));

        // 预设按钮
        setupPresetButtons();

        // 历史记录按钮
        findViewById(R.id.btn_clear_history).setOnClickListener(v -> clearHistory());
    }

    /**
     * 设置预设按钮监听器
     */
    private void setupPresetButtons() {
        // 基本单位
        findViewById(R.id.btn_preset_1s).setOnClickListener(v -> setPresetValue(1, 3));  // 1秒
        findViewById(R.id.btn_preset_1m).setOnClickListener(v -> setPresetValue(1, 4));  // 1分钟
        findViewById(R.id.btn_preset_1h).setOnClickListener(v -> setPresetValue(1, 5));  // 1小时
        findViewById(R.id.btn_preset_1d).setOnClickListener(v -> setPresetValue(1, 6));  // 1天
        findViewById(R.id.btn_preset_1w).setOnClickListener(v -> setPresetValue(1, 7));  // 1周
        findViewById(R.id.btn_preset_1mon).setOnClickListener(v -> setPresetValue(1, 8)); // 1月
        findViewById(R.id.btn_preset_1y).setOnClickListener(v -> setPresetValue(1, 9));  // 1年

        // 特殊单位
        findViewById(R.id.btn_preset_workday).setOnClickListener(v -> setWorkdayPreset());
        findViewById(R.id.btn_preset_leapyear).setOnClickListener(v -> setLeapYearPreset());
    }

    /**
     * 设置预设值
     */
    private void setPresetValue(double value, int unitIndex) {
        etInputValue.setText(String.valueOf((int)value));
        spinnerFromUnit.setSelection(unitIndex);

        String unitName = TIME_UNITS[unitIndex];
        Toast.makeText(this, "已设置为: " + (int)value + " " + unitName, Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置工作日预设（8小时）
     */
    private void setWorkdayPreset() {
        etInputValue.setText("8");
        spinnerFromUnit.setSelection(5); // 小时

        // 自动设置到天
        spinnerToUnit.setSelection(6); // 天

        Toast.makeText(this, "已设置为: 1工作日 (8小时)", Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置闰年预设
     */
    private void setLeapYearPreset() {
        etInputValue.setText("366");
        spinnerFromUnit.setSelection(6); // 天

        Toast.makeText(this, "已设置为: 1闰年 (366天)", Toast.LENGTH_SHORT).show();
    }

    /**
     * 交换单位
     */
    private void swapUnits() {
        // 交换单位索引
        int temp = fromUnitIndex;
        fromUnitIndex = toUnitIndex;
        toUnitIndex = temp;

        // 交换Spinner选择
        spinnerFromUnit.setSelection(fromUnitIndex);
        spinnerToUnit.setSelection(toUnitIndex);

        // 如果输出值不为空，交换输入输出值
        String outputValue = tvOutputValue.getText().toString();
        if (!outputValue.isEmpty() && !outputValue.equals("0")) {
            try {
                double newInputValue = Double.parseDouble(outputValue);
                etInputValue.setText(formatNumber(newInputValue, precision));
                calculateConversion();
            } catch (NumberFormatException e) {
                // 忽略格式错误
            }
        }

        Toast.makeText(this, "已交换换算单位", Toast.LENGTH_SHORT).show();
    }

    /**
     * 复制结果到剪贴板
     */
    private void copyResult() {
        String result = tvMainResult.getText().toString();
        if (!result.isEmpty()) {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("时间换算结果", result);
            clipboard.setPrimaryClip(clip);

            Toast.makeText(this, "结果已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 清空输入
     */
    private void clearInput() {
        etInputValue.setText("");
        inputValue = 0;
        calculateConversion();

        Toast.makeText(this, "输入已清空", Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置输入为1
     */
    private void setInputToOne() {
        etInputValue.setText("1");
        inputValue = 1;
        calculateConversion();

        Toast.makeText(this, "输入值已设为1", Toast.LENGTH_SHORT).show();
    }

    /**
     * 设置显示精度
     */
    private void setPrecision(int newPrecision) {
        precision = newPrecision;

        // 更新按钮状态
        updatePrecisionButtons();

        // 重新计算显示
        calculateConversion();
        savePreference();

        String[] precisionNames = {"低", "中", "高"};
        Toast.makeText(this, "显示精度已设为: " + precisionNames[precision], Toast.LENGTH_SHORT).show();
    }

    /**
     * 更新精度按钮状态
     */
    private void updatePrecisionButtons() {
        int colorLow = precision == 0 ? 0xFF3F51B5 : 0xFFBDBDBD;
        int colorMedium = precision == 1 ? 0xFF3F51B5 : 0xFFBDBDBD;
        int colorHigh = precision == 2 ? 0xFF3F51B5 : 0xFFBDBDBD;

        findViewById(R.id.btn_precision_low).setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(colorLow));
        findViewById(R.id.btn_precision_medium).setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(colorMedium));
        findViewById(R.id.btn_precision_high).setBackgroundTintList(
                android.content.res.ColorStateList.valueOf(colorHigh));
    }

    /**
     * 计算单位换算
     */
    private void calculateConversion() {
        if (inputValue == 0) {
            tvOutputValue.setText("0");
            tvMainResult.setText("请输入数值");
            layoutResultsGrid.removeAllViews();
            return;
        }

        try {
            // 计算主要换算结果
            double result = convertValue(inputValue, fromUnitIndex, toUnitIndex);
            String formattedResult = formatNumber(result, precision);

            // 更新显示
            tvOutputValue.setText(formattedResult);
            tvMainResult.setText(formatConversionText(inputValue, fromUnitIndex, result, toUnitIndex));

            // 生成详细换算结果
            generateDetailedResults();

            // 保存到历史记录
            addToHistory();

        } catch (Exception e) {
            tvOutputValue.setText("错误");
            tvMainResult.setText("计算错误");
            layoutResultsGrid.removeAllViews();
        }
    }

    /**
     * 单位换算计算
     */
    private double convertValue(double value, int fromUnit, int toUnit) {
        // 先将输入值转换为秒
        double valueInSeconds = value / UNIT_FACTORS[fromUnit];

        // 再从秒转换为目标单位
        return valueInSeconds * UNIT_FACTORS[toUnit];
    }

    /**
     * 格式化数字
     */
    private String formatNumber(double number, int precision) {
        if (Double.isInfinite(number) || Double.isNaN(number)) {
            return "无限大";
        }

        String pattern;
        switch (precision) {
            case 0: // 低精度：整数
                pattern = "#,##0";
                break;
            case 1: // 中精度：2位小数
                pattern = "#,##0.##";
                break;
            case 2: // 高精度：6位小数
                pattern = "#,##0.######";
                break;
            default:
                pattern = "#,##0.##";
        }

        DecimalFormat df = new DecimalFormat(pattern);
        return df.format(number);
    }

    /**
     * 格式化换算文本
     */
    private String formatConversionText(double fromValue, int fromUnit, double toValue, int toUnit) {
        String fromUnitName = TIME_UNITS[fromUnit];
        String toUnitName = TIME_UNITS[toUnit];

        String fromStr = formatNumber(fromValue, precision) + " " + fromUnitName;
        String toStr = formatNumber(toValue, precision) + " " + toUnitName;

        return fromStr + " = " + toStr;
    }

    /**
     * 生成详细换算结果
     */
    private void generateDetailedResults() {
        // 清空现有结果
        layoutResultsGrid.removeAllViews();

        // 计算所有单位的换算值
        double baseValueInSeconds = inputValue / UNIT_FACTORS[fromUnitIndex];

        // 添加标题行
        addResultRow("单位", "换算值", true);

        // 添加所有单位的结果
        for (int i = 0; i < TIME_UNITS.length; i++) {
            if (i != fromUnitIndex) { // 跳过输入单位本身
                double convertedValue = baseValueInSeconds * UNIT_FACTORS[i];
                String formattedValue = formatNumber(convertedValue, precision);
                addResultRow(TIME_UNITS[i], formattedValue, false);
            }
        }

        // 添加特殊单位结果
        addSpecialUnitsResults(baseValueInSeconds);
    }

    /**
     * 添加特殊单位结果
     */
    private void addSpecialUnitsResults(double baseValueInSeconds) {
        // 工作日（按8小时计算）
        double workdays = baseValueInSeconds * UNIT_FACTORS[5] / WORKDAY_HOURS; // 转换为小时，再除以8
        addResultRow("工作日", formatNumber(workdays, precision), false);

        // 闰年天数
        double leapYearDays = baseValueInSeconds * UNIT_FACTORS[6] / LEAP_YEAR_DAYS; // 转换为天，再除以366
        addResultRow("闰年天数", formatNumber(leapYearDays, precision), false);
    }

    /**
     * 添加结果行到网格
     */
    private void addResultRow(String unitName, String value, boolean isHeader) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.item_result_row, layoutResultsGrid, false);

        TextView tvUnit = row.findViewById(R.id.tv_result_unit);
        TextView tvValue = row.findViewById(R.id.tv_result_value);

        tvUnit.setText(unitName);
        tvValue.setText(value);

        if (isHeader) {
            tvUnit.setTextColor(Color.parseColor("#3F51B5"));
            tvValue.setTextColor(Color.parseColor("#3F51B5"));
            tvUnit.setTextSize(14);
            tvValue.setTextSize(14);
            row.setBackgroundColor(Color.parseColor("#F5F5F5"));
        }

        layoutResultsGrid.addView(row);
    }

    /**
     * 添加到历史记录
     */
    private void addToHistory() {
        if (inputValue == 0) return;

        String fromValue = formatNumber(inputValue, precision);
        String fromUnit = TIME_UNITS[fromUnitIndex];
        String toValue = tvOutputValue.getText().toString();
        String toUnit = TIME_UNITS[toUnitIndex];

        HistoryItem item = new HistoryItem(fromValue, fromUnit, toValue, toUnit);
        historyList.add(0, item); // 添加到开头

        // 限制历史记录数量
        if (historyList.size() > MAX_HISTORY) {
            historyList.remove(historyList.size() - 1);
        }

        // 更新历史显示
        updateHistoryDisplay();

        // 保存历史记录
        saveHistory();
    }

    /**
     * 更新历史记录显示
     */
    private void updateHistoryDisplay() {
        if (historyList.isEmpty()) {
            listHistory.setVisibility(View.GONE);
            tvNoHistory.setVisibility(View.VISIBLE);
            return;
        }

        listHistory.setVisibility(View.VISIBLE);
        tvNoHistory.setVisibility(View.GONE);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                R.layout.item_history,
                R.id.tv_history_item
        ) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);

                HistoryItem item = historyList.get(position);

                TextView tvItem = view.findViewById(R.id.tv_history_item);
                TextView tvTime = view.findViewById(R.id.tv_history_time);

                tvItem.setText(item.getDisplayText());
                tvTime.setText(item.timestamp);

                return view;
            }

            @Override
            public int getCount() {
                return historyList.size();
            }

            @Override
            public String getItem(int position) {
                return historyList.get(position).getDisplayText();
            }
        };

        listHistory.setAdapter(adapter);

        // 点击历史项恢复
        listHistory.setOnItemClickListener((parent, view, position, id) -> {
            HistoryItem item = historyList.get(position);
            etInputValue.setText(item.fromValue);
            spinnerFromUnit.setSelection(getUnitIndex(item.fromUnit));
            spinnerToUnit.setSelection(getUnitIndex(item.toUnit));

            Toast.makeText(this, "已恢复历史记录", Toast.LENGTH_SHORT).show();
        });
    }

    /**
     * 获取单位索引
     */
    private int getUnitIndex(String unitName) {
        for (int i = 0; i < TIME_UNITS.length; i++) {
            if (TIME_UNITS[i].equals(unitName)) {
                return i;
            }
        }
        return 3; // 默认返回秒
    }

    /**
     * 清空历史记录
     */
    private void clearHistory() {
        if (historyList.isEmpty()) {
            Toast.makeText(this, "历史记录已为空", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("确认清空");
        builder.setMessage("确定要清空所有历史记录吗？");
        builder.setPositiveButton("清空", (dialog, which) -> {
            historyList.clear();
            updateHistoryDisplay();
            saveHistory();
            Toast.makeText(this, "历史记录已清空", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }

    /**
     * 显示帮助对话框
     */
    private void showHelpDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("时间单位换算帮助");

        String helpText = "使用方法：\n\n" +
                "1. 输入数值：在输入框中输入要换算的数值\n" +
                "2. 选择单位：分别选择输入和输出的时间单位\n" +
                "3. 查看结果：自动实时计算并显示换算结果\n\n" +
                "功能说明：\n" +
                "• 支持纳秒、微秒、毫秒、秒、分钟、小时、天、周、月、年\n" +
                "• 月按30天计算，年按365天计算\n" +
                "• 工作日按8小时计算，闰年按366天计算\n" +
                "• 支持小数输入和显示精度调整\n" +
                "• 可交换输入输出单位\n" +
                "• 自动保存历史记录（最多20条）\n\n" +
                "换算基准：\n" +
                "• 1秒 = 1,000毫秒 = 1,000,000微秒\n" +
                "• 1分钟 = 60秒\n" +
                "• 1小时 = 60分钟\n" +
                "• 1天 = 24小时\n" +
                "• 1周 = 7天\n" +
                "• 1月 ≈ 30天\n" +
                "• 1年 ≈ 365天";

        builder.setMessage(helpText);
        builder.setPositiveButton("确定", null);
        builder.setNeutralButton("查看示例", (dialog, which) -> showExample());

        builder.show();
    }

    /**
     * 显示示例
     */
    private void showExample() {
        // 设置示例：1周换算
        etInputValue.setText("1");
        spinnerFromUnit.setSelection(7); // 周
        spinnerToUnit.setSelection(6);   // 天

        calculateConversion();

        Toast.makeText(this, "已加载示例：1周换算", Toast.LENGTH_SHORT).show();
    }

    /**
     * 保存数据
     */
    private void saveInputValue() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PREF_INPUT_VALUE, etInputValue.getText().toString());
        editor.apply();
    }

    private void saveFromUnit() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_FROM_UNIT, fromUnitIndex);
        editor.apply();
    }

    private void saveToUnit() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_TO_UNIT, toUnitIndex);
        editor.apply();
    }

    private void savePreference() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(PREF_PRECISION, precision);
        editor.apply();
    }

    /**
     * 加载保存的数据
     */
    private void loadSavedData() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        // 加载输入值
        String savedInputValue = prefs.getString(PREF_INPUT_VALUE, "1");
        etInputValue.setText(savedInputValue);
        try {
            inputValue = Double.parseDouble(savedInputValue);
        } catch (NumberFormatException e) {
            inputValue = 1;
        }

        // 加载单位
        fromUnitIndex = prefs.getInt(PREF_FROM_UNIT, 7); // 默认周
        toUnitIndex = prefs.getInt(PREF_TO_UNIT, 3);     // 默认天

        spinnerFromUnit.setSelection(fromUnitIndex);
        spinnerToUnit.setSelection(toUnitIndex);

        // 加载精度
        precision = prefs.getInt(PREF_PRECISION, 1); // 默认中精度
        updatePrecisionButtons();
    }

    /**
     * 保存历史记录
     */
    private void saveHistory() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (HistoryItem item : historyList) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("fromValue", item.fromValue);
                jsonObject.put("fromUnit", item.fromUnit);
                jsonObject.put("toValue", item.toValue);
                jsonObject.put("toUnit", item.toUnit);
                jsonObject.put("timestamp", item.timestamp);
                jsonArray.put(jsonObject);
            }

            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(PREF_HISTORY, jsonArray.toString());
            editor.apply();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载历史记录
     */
    private void loadHistory() {
        historyList.clear();

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String historyJson = prefs.getString(PREF_HISTORY, "");

        if (!historyJson.isEmpty()) {
            try {
                JSONArray jsonArray = new JSONArray(historyJson);
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    HistoryItem item = new HistoryItem(
                            jsonObject.getString("fromValue"),
                            jsonObject.getString("fromUnit"),
                            jsonObject.getString("toValue"),
                            jsonObject.getString("toUnit")
                    );
                    item.timestamp = jsonObject.getString("timestamp");
                    historyList.add(item);
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        updateHistoryDisplay();
    }

    /**
     * 处理返回键
     */
    @Override
    public void onBackPressed() {
        // 保存当前数据
        saveInputValue();
        saveFromUnit();
        saveToUnit();
        savePreference();
        saveHistory();

        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /**
     * 保存状态
     */
    @Override
    protected void onPause() {
        super.onPause();
        saveInputValue();
        saveFromUnit();
        saveToUnit();
        savePreference();
        saveHistory();
    }
}