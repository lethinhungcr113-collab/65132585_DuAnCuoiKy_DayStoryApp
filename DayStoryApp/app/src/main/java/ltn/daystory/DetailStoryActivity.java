package ltn.daystory;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class DetailStoryActivity extends AppCompatActivity {

    private ImageView imgDetail;
    private TextView txtNgay, txtNoiDung;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_story);

        // --- BƯỚC 1: ÁNH XẠ ---
        imgDetail = findViewById(R.id.imgDetail);
        txtNgay = findViewById(R.id.txtNgay);
        txtNoiDung = findViewById(R.id.txtNoiDung);
        View nutQuayLai = findViewById(R.id.btnBack);

        // --- BƯỚC 2: KIỂM TRA NULL TRƯỚC KHI DÙNG ---
        if (nutQuayLai != null) {
            nutQuayLai.setOnClickListener(v -> finish());
        }

        // --- BƯỚC 3: NHẬN DỮ LIỆU ---
        if (getIntent() != null) {
            String noiDung = getIntent().getStringExtra("noiDung");
            String ngay = getIntent().getStringExtra("ngay");
            String anh = getIntent().getStringExtra("anh");

            // Hiển thị chữ
            if (txtNoiDung != null) txtNoiDung.setText(noiDung);
            if (txtNgay != null) txtNgay.setText(ngay);

            // Hiển thị ảnh
            if (anh != null && !anh.isEmpty() && imgDetail != null) {
                try {
                    byte[] decodedString = Base64.decode(anh, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                    imgDetail.setImageBitmap(bitmap);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}