package com.example.cookmate.ui.inventory;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.example.cookmate.R;
import com.example.cookmate.network.ServerClient;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class AddFoodActivity extends AppCompatActivity {

    private EditText etName, etQuantity, etPurchaseDate;
    private Spinner spinnerCategory;
    private Button btnSave, btnTakePhoto;
    private ImageView ivPreview;
    private Calendar selectedDate = Calendar.getInstance();
    private String photoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_food);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        etName = findViewById(R.id.et_name);
        etQuantity = findViewById(R.id.et_quantity);
        etPurchaseDate = findViewById(R.id.et_purchase_date);
        spinnerCategory = findViewById(R.id.spinner_category);
        btnSave = findViewById(R.id.btn_save);
        btnTakePhoto = findViewById(R.id.btn_take_photo);
        ivPreview = findViewById(R.id.iv_preview);

        setDefaultIcon("其他");

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int pos, long id) {
                if (photoPath == null) setDefaultIcon(parent.getItemAtPosition(pos).toString());
            }
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        etPurchaseDate.setText(sdf.format(new Date()));

        etPurchaseDate.setOnClickListener(v -> {
            new android.app.DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        selectedDate.set(year, month, dayOfMonth);
                        etPurchaseDate.setText(sdf.format(selectedDate.getTime()));
                    },
                    selectedDate.get(Calendar.YEAR),
                    selectedDate.get(Calendar.MONTH),
                    selectedDate.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnTakePhoto.setOnClickListener(v -> openGallery());
        btnSave.setOnClickListener(v -> saveFoodItem());
    }

    private void setDefaultIcon(String category) {
        int resId;
        switch (category) {
            case "蔬菜": resId = R.drawable.ic_vegetable; break;
            case "水果": resId = R.drawable.ic_fruit; break;
            case "肉类": resId = R.drawable.ic_meat; break;
            case "乳制品": resId = R.drawable.ic_dairy; break;
            case "调料": resId = R.drawable.ic_seasoning; break;
            default: resId = R.drawable.ic_food_default; break;
        }
        ivPreview.setImageResource(resId);
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, 300);
    }

    private long getUserId() {
        SharedPreferences sp = getSharedPreferences("user", MODE_PRIVATE);
        return sp.getLong("userId", 1);
    }

    private void saveFoodItem() {
        String name = etName.getText().toString().trim();
        String quantity = etQuantity.getText().toString().trim();
        String category = spinnerCategory.getSelectedItem().toString();
        String dateStr = etPurchaseDate.getText().toString().trim();

        if ("请选择类别".equals(category)) {
            Toast.makeText(this, "请选择类别", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.isEmpty()) {
            Toast.makeText(this, "请输入食材名称", Toast.LENGTH_SHORT).show();
            return;
        }

        ServerClient.addFoodItem(getUserId(), name, category, quantity.isEmpty() ? "1份" : quantity,
                dateStr, dateStr, new ServerClient.OnResultListener() {
                    @Override
                    public void onSuccess(String json) {
                        runOnUiThread(() -> {
                            Toast.makeText(AddFoodActivity.this, "添加成功", Toast.LENGTH_SHORT).show();
                            finish();
                        });
                    }
                    @Override
                    public void onError(String error) {
                        runOnUiThread(() -> Toast.makeText(AddFoodActivity.this, "添加失败: " + error, Toast.LENGTH_SHORT).show());
                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 300 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            ivPreview.setImageURI(uri);
            photoPath = uri.toString();
        }
    }
}