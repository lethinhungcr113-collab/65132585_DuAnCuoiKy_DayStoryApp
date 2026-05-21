package ltn.daystory;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class AnalyticsActivity extends AppCompatActivity {

    private TextView txtTotalDiaries;
    private TextView txtStreak;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        androidx.activity.EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analytics);

        // Ánh xạ các View số liệu
        txtTotalDiaries = findViewById(R.id.txtTotalDiaries);
        txtStreak = findViewById(R.id.txtStreak);

        setupBottomNavigation();
        loadThongKeTuFirebase();
    }

    private void setupBottomNavigation() {
        LinearLayout navHome = findViewById(R.id.navHome);
        LinearLayout navCalendar = findViewById(R.id.navCalendar);

        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(AnalyticsActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });

        navCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(AnalyticsActivity.this, CalendarActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
            finish();
        });
    }

    private void loadThongKeTuFirebase() {
        db.collection("DanhSachNhatKy")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        // 1. Hiển thị tổng số bài viết
                        int total = queryDocumentSnapshots.size();
                        txtTotalDiaries.setText(String.valueOf(total));

                        // 2. Gom tất cả các ngày viết nhật ký vào một danh sách để tính Streak
                        List<Date> danhSachNgay = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                            Date date = doc.getDate("ngayThang");
                            if (date != null) {
                                danhSachNgay.add(date);
                            }
                        }

                        // 3. Gọi hàm tính chuỗi ngày liên tiếp và hiển thị lên màn hình
                        int streak = tinhChuoiNgayLienTiep(danhSachNgay);
                        txtStreak.setText(streak + " ngày");
                    } else {
                        txtTotalDiaries.setText("0");
                        txtStreak.setText("0 ngày");
                    }
                });
    }

    private int tinhChuoiNgayLienTiep(List<Date> danhSachNgay) {
        if (danhSachNgay == null || danhSachNgay.isEmpty()) return 0;

        // Bỏ giờ phút giây, chỉ lấy phần ngày-tháng-năm dạng số Nguyên (mili-giây) để tính toán chính xác
        List<Long> danhSachMiliNgay = new ArrayList<>();
        for (Date d : danhSachNgay) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            if (!danhSachMiliNgay.contains(cal.getTimeInMillis())) {
                danhSachMiliNgay.add(cal.getTimeInMillis());
            }
        }

        // Sắp xếp danh sách ngày từ Cũ nhất đến Mới nhất
        Collections.sort(danhSachMiliNgay);

        // Lấy ngày hôm nay (đặt về mùng 0h00)
        Calendar calHomNay = Calendar.getInstance();
        calHomNay.set(Calendar.HOUR_OF_DAY, 0);
        calHomNay.set(Calendar.MINUTE, 0);
        calHomNay.set(Calendar.SECOND, 0);
        calHomNay.set(Calendar.MILLISECOND, 0);
        long miliHomNay = calHomNay.getTimeInMillis();

        // Lấy ngày hôm qua
        calHomNay.add(Calendar.DAY_OF_YEAR, -1);
        long miliHomQua = calHomNay.getTimeInMillis();

        // Kiểm tra xem bài viết mới nhất có phải là hôm nay hoặc hôm qua không.
        // Nếu ngày cuối cùng viết nhật ký còn cũ hơn cả hôm qua -> Chuỗi liên tiếp coi như bằng 0 (đứt chuỗi)
        long ngayCuoiCungTrongDanhSach = danhSachMiliNgay.get(danhSachMiliNgay.size() - 1);
        if (ngayCuoiCungTrongDanhSach < miliHomQua) {
            return 0;
        }

        int streakCount = 1; // Khởi tạo chuỗi bắt đầu từ 1 ngày

        // Duyệt ngược từ cuối danh sách (ngày gần nhất) lùi dần về quá khứ
        for (int i = danhSachMiliNgay.size() - 1; i > 0; i--) {
            long ngaySau = danhSachMiliNgay.get(i);
            long ngayTruoc = danhSachMiliNgay.get(i - 1);

            // Tính khoảng cách thời gian giữa 2 bài viết kề nhau (quy đổi ra số ngày)
            long diffInMili = ngaySau - ngayTruoc;
            long diffInDays = diffInMili / (24 * 60 * 60 * 1000);

            if (diffInDays == 1) {
                // Nếu cách nhau đúng 1 ngày -> Chuỗi liên tiếp được nối dài
                streakCount++;
            } else if (diffInDays > 1) {
                // Bị cách quãng từ 2 ngày trở lên -> Chuỗi liên tiếp bị đứt, dừng vòng lặp ngay
                break;
            }
            // Trường hợp diffInDays == 0 nghĩa là cùng một ngày viết nhiều bài
        }

        return streakCount;
    }
}