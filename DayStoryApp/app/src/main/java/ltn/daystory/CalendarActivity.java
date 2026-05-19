package ltn.daystory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {
    private TextView txtMonthYear;
    private RecyclerView rvCalendar;
    private Calendar selectedDate;

    // KHAI BÁO CÁC BIẾN CHO FIREBASE VÀ ADAPTER
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ArrayList<Integer> danhSachNgayCoNote = new ArrayList<>();
    private ArrayList<String> danhSachNgay = new ArrayList<>();
    private CalendarAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        anhXaView();
        setupBottomNavigation(); // Kích hoạt thanh điều hướng dưới đáy màn hình

        // Mặc định khi mở màn hình sẽ lấy tháng hiện tại của điện thoại
        selectedDate = Calendar.getInstance();

        // KHỞI TẠO ADAPTER
        adapter = new CalendarAdapter(danhSachNgay, danhSachNgayCoNote, dayText -> {
            if (dayText != null && !dayText.isEmpty()) {
                int dayClicked = Integer.parseInt(dayText);
                adapter.updateSelectedDay(dayClicked); // Kích hoạt hiệu ứng tô đặc ngày được chọn
                layBaiVietPreviewTheoNgay(dayClicked);  // Tìm bài viết cụ thể của ngày vừa chọn để hiển thị xem trước
            }
        });

        rvCalendar.setLayoutManager(new GridLayoutManager(CalendarActivity.this, 7));
        rvCalendar.setAdapter(adapter);

        // Gọi hàm để đổ dữ liệu tháng đầu tiên lên lịch
        hienThiLich();

        // Xử lý sự kiện nút bấm điều hướng tháng
        ImageButton btnPrev = findViewById(R.id.btnPrev);
        ImageButton btnNext = findViewById(R.id.btnNext);

        btnPrev.setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, -1); // Trừ đi 1 tháng
            hienThiLich(); // Vẽ lại lịch với dữ liệu tháng mới
        });

        btnNext.setOnClickListener(v -> {
            selectedDate.add(Calendar.MONTH, 1); // Cộng thêm 1 tháng
            hienThiLich(); // Vẽ lại lịch với dữ liệu tháng mới
        });
    }

    private void anhXaView() {
        txtMonthYear = findViewById(R.id.txtMonthYear);
        rvCalendar = findViewById(R.id.rvCalendar);
    }

    // THIẾT LẬP THANH ĐIỀU HƯỚNG DƯỚI ĐÁY ĐỂ CHUYỂN QUA LẠI GIỮA CÁC MÀN HÌNH
    private void setupBottomNavigation() {
        LinearLayout navAnalytics = findViewById(R.id.navAnalytics);
        LinearLayout navCalendar = findViewById(R.id.navCalendar);
        LinearLayout navHome = findViewById(R.id.navHome);

        // Bấm nút Nhật ký -> Quay trở lại màn hình chính MainActivity
        navHome.setOnClickListener(v -> {
            Intent intent = new Intent(CalendarActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // Bấm nút Thống kê
        navAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnalyticsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0); // Hiệu ứng giữ nguyên menu lơ lửng không giật lag
        });

        // Click nút Lịch thực hiện cuộn mượt lên dòng đầu tiên của tháng đang hiển thị
        navCalendar.setOnClickListener(v -> {
            if (rvCalendar != null) {
                rvCalendar.smoothScrollToPosition(0);
            }
        });
    }

    private void hienThiLich() {
        // 1. Cập nhật chữ hiển thị Tháng/Năm lên Header
        txtMonthYear.setText(dinhDangThangNam(selectedDate));

        // 2. Cập nhật lại danh sách ma trận các ngày của tháng mới vào list toàn cục
        danhSachNgay.clear();
        danhSachNgay.addAll(taoDanhSachNgayTrongThang(selectedDate));

        // Reset ngày đang chọn về mặc định khi sang tháng mới để không bị giữ màu cũ
        adapter.updateSelectedDay(-1);

        // 3. TÍNH TOÁN KHOẢNG THỜI GIAN ĐẦU THÁNG VÀ CUỐI THÁNG ĐỂ LỌC FIREBASE
        Calendar startCal = (Calendar) selectedDate.clone();
        startCal.set(Calendar.DAY_OF_MONTH, 1);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        Date tuNgay = startCal.getTime();

        Calendar endCal = (Calendar) selectedDate.clone();
        endCal.set(Calendar.DAY_OF_MONTH, endCal.getActualMaximum(Calendar.DAY_OF_MONTH));
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        Date denNgay = endCal.getTime();

        // 4. TRUY VẤN FIREBASE LẤY CÁC BÀI VIẾT TRONG THÁNG ĐANG XEM
        danhSachNgayCoNote.clear();
        adapter.notifyDataSetChanged();

        db.collection("DanhSachNhatKy")
                .whereGreaterThanOrEqualTo("ngayThang", tuNgay)
                .whereLessThanOrEqualTo("ngayThang", denNgay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Date dateInFirebase = doc.getDate("ngayThang");
                        if (dateInFirebase != null) {
                            Calendar c = Calendar.getInstance();
                            c.setTime(dateInFirebase);
                            int day = c.get(Calendar.DAY_OF_MONTH);
                            if (!danhSachNgayCoNote.contains(day)) {
                                danhSachNgayCoNote.add(day); // Lưu ngày có bài viết
                            }
                        }
                    }

                    // 5. CẬP NHẬT LẠI GIAO DIỆN
                    adapter.notifyDataSetChanged();
                });
    }

    // Hàm lấy bài viết xem trước (Preview) ở khu vực bên dưới lịch khi bấm chọn ngày
    private void layBaiVietPreviewTheoNgay(int day) {
        Calendar targetCal = (Calendar) selectedDate.clone();
        targetCal.set(Calendar.DAY_OF_MONTH, day);

        // Giới hạn thời gian từ 00:00:00 đến 23:59:59 của ngày được chọn
        targetCal.set(Calendar.HOUR_OF_DAY, 0);
        targetCal.set(Calendar.MINUTE, 0);
        targetCal.set(Calendar.SECOND, 0);
        Date startDay = targetCal.getTime();

        targetCal.set(Calendar.HOUR_OF_DAY, 23);
        targetCal.set(Calendar.MINUTE, 59);
        targetCal.set(Calendar.SECOND, 59);
        Date endDay = targetCal.getTime();

        db.collection("DanhSachNhatKy")
                .whereGreaterThanOrEqualTo("ngayThang", startDay)
                .whereLessThanOrEqualTo("ngayThang", endDay)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    TextView txtNoEntry = findViewById(R.id.txtNoEntry);
                    FrameLayout layoutPreview = findViewById(R.id.layoutPreview);

                    if (!queryDocumentSnapshots.isEmpty()) {
                        txtNoEntry.setVisibility(View.GONE);
                        layoutPreview.setVisibility(View.VISIBLE);

                        // Lấy tài liệu đầu tiên của ngày đó để làm nội dung hiển thị thử
                        QueryDocumentSnapshot doc = (QueryDocumentSnapshot) queryDocumentSnapshots.getDocuments().get(0);
                        String noiDung = doc.getString("noiDung");

                        Toast.makeText(this, "Nội dung: " + noiDung, Toast.LENGTH_SHORT).show();
                    } else {
                        txtNoEntry.setVisibility(View.VISIBLE);
                        layoutPreview.setVisibility(View.GONE);
                    }
                });
    }

    // Hàm định dạng hiển thị Tháng Năm
    private String dinhDangThangNam(Calendar date) {
        SimpleDateFormat formatter = new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH);
        return formatter.format(date.getTime());
    }

    // THUẬT TOÁN TÍNH TOÁN Ô TRỐNG VÀ SỐ NGÀY ĐẦU THÁNG CHUẨN ĐỊNH DẠNG LỊCH US
    private ArrayList<String> taoDanhSachNgayTrongThang(Calendar date) {
        ArrayList<String> daysInMonthArray = new ArrayList<>();

        Calendar cal = Calendar.getInstance(Locale.US);
        cal.setTime(date.getTime());

        cal.set(Calendar.DAY_OF_MONTH, 1);
        int maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

        int oTrongPhiaTruoc = dayOfWeek - 1;

        for (int i = 1; i <= oTrongPhiaTruoc; i++) {
            daysInMonthArray.add("");
        }

        for (int i = 1; i <= maxDay; i++) {
            daysInMonthArray.add(String.valueOf(i));
        }

        while (daysInMonthArray.size() < 42) {
            daysInMonthArray.add("");
        }

        return daysInMonthArray;
    }
}