package ltn.daystory;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private FirebaseFirestore duLieu;
    private NhatKyAdapter adapter;
    private List<NhatKy> danhSachNhatKy;
    private ListenerRegistration listenerNhatKy;
    private boolean sapXepMoiNhat = true;

    private RecyclerView rvDiaries;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        setContentView(R.layout.activity_main);

        // ÁNH XẠ VIEW GIAO DIỆN CHÍNH
        rvDiaries = findViewById(R.id.rvDiaries);
        View nutThem = findViewById(R.id.btnOpenAdd);
        MaterialCardView btnSort = findViewById(R.id.btnSort);

        // 1. Ánh xạ các nút điều hướng Bottom Menu
        LinearLayout navAnalytics = findViewById(R.id.navAnalytics);
        LinearLayout navCalendar = findViewById(R.id.navCalendar);
        LinearLayout navHome = findViewById(R.id.navHome);

        // 2. Bấm nút Lịch (Bên phải)
        navCalendar.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, CalendarActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // 3. Bấm nút Thống kê (Bên trái)
        navAnalytics.setOnClickListener(v -> {
            Intent intent = new Intent(this, AnalyticsActivity.class);
            startActivity(intent);
            overridePendingTransition(0, 0);
        });

        // 4. Bấm nút chính giữa
        navHome.setOnClickListener(v -> {
            if (danhSachNhatKy != null && !danhSachNhatKy.isEmpty()) {
                rvDiaries.smoothScrollToPosition(0);
            }
        });

        // SYSTEM BAR
        if (findViewById(R.id.main) != null) {
            ViewCompat.setOnApplyWindowInsetsListener(
                    findViewById(R.id.main),
                    (v, insets) -> {
                        Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                        v.setPadding(systemBars.left, 0, systemBars.right, 0);
                        return insets;
                    }
            );
        }

        // KẾT NỐI DATABASE VÀ ĐỔ DỮ LIỆU RECYCLERVIEW
        duLieu = FirebaseFirestore.getInstance();
        danhSachNhatKy = new ArrayList<>();
        adapter = new NhatKyAdapter(this, danhSachNhatKy);

        rvDiaries.setLayoutManager(new LinearLayoutManager(this));
        rvDiaries.setAdapter(adapter);

        layDuLieuTuFirebase(Query.Direction.DESCENDING);

        // NÚT LỌC SẮP XẾP
        btnSort.setOnClickListener(v -> {
            showFilterMenu(v);
        });

        // NÚT MỞ MÀN HÌNH THÊM BÀI VIẾT MỚI
        nutThem.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddStoryActivity.class);
            startActivity(intent);
        });
    }

    // LẤY DỮ LIỆU FIREBASE
    private void layDuLieuTuFirebase(Query.Direction huongSapXep) {
        // Hủy listener cũ nếu có
        if (listenerNhatKy != null) {
            listenerNhatKy.remove();
        }

        listenerNhatKy = duLieu
                .collection("DanhSachNhatKy")
                .orderBy("ngayThang", huongSapXep)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        error.printStackTrace();
                        return;
                    }

                    if (value != null) {
                        try {
                            danhSachNhatKy.clear();
                            for (DocumentSnapshot doc : value.getDocuments()) {
                                NhatKy nhatKy = doc.toObject(NhatKy.class);
                                if (nhatKy != null) {
                                    nhatKy.setDocumentId(doc.getId());
                                    danhSachNhatKy.add(nhatKy);
                                }
                            }
                            adapter.notifyDataSetChanged();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listenerNhatKy != null) {
            listenerNhatKy.remove();
        }
    }

    // HÀM HIỂN THỊ MENU LỌC (POPUP)
    private void showFilterMenu(View v) {
        View menuView = getLayoutInflater().inflate(R.layout.layout_filter_menu, null);
        android.widget.PopupWindow popupWindow = new android.widget.PopupWindow(menuView, 500,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        popupWindow.setElevation(20);
        popupWindow.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));

        // Xử lý click nút Mới nhất
        menuView.findViewById(R.id.btnSortNewest).setOnClickListener(view -> {
            sapXepMoiNhat = true;
            layDuLieuTuFirebase(Query.Direction.DESCENDING);
            popupWindow.dismiss();
        });

        // Xử lý click nút Cũ nhất
        menuView.findViewById(R.id.btnSortOldest).setOnClickListener(view -> {
            sapXepMoiNhat = false;
            layDuLieuTuFirebase(Query.Direction.ASCENDING);
            popupWindow.dismiss();
        });

        popupWindow.showAsDropDown(v, 0, 10);
    }
}