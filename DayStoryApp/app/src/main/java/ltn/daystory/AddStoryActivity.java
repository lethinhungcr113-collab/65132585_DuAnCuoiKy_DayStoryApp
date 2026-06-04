package ltn.daystory;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class AddStoryActivity extends AppCompatActivity {
    private ActivityResultLauncher<Intent> boChupAnh;
    private ActivityResultLauncher<Intent> boChonAnh;
    private ImageView imgSelected;
    private EditText edtContent;
    private MaterialButton btnSave;
    private View layoutUploadInfo;
    private Bitmap bitmapAnhChup = null;
    private boolean isEditMode = false;
    private String documentId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.addstory_activity);

        anhXaView();
        setupLaunchers();
        setupClickEvents();
        kiemTraCheDoSua();
    }

    private void anhXaView() {
        imgSelected = findViewById(R.id.imgSelected);
        edtContent = findViewById(R.id.edtContent);
        btnSave = findViewById(R.id.btnSave);
        layoutUploadInfo = findViewById(R.id.layoutUploadInfo);
        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());
    }

    private void setupLaunchers() {
        boChupAnh = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                ketQua -> {
                    if (ketQua.getResultCode() == RESULT_OK && ketQua.getData() != null) {
                        Bundle extras = ketQua.getData().getExtras();
                        if (extras != null) {
                            bitmapAnhChup = (Bitmap) extras.get("data");
                            hienThiAnhDaChon();
                        }
                    }
                });
        boChonAnh = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                ketQua -> {
                    if (ketQua.getResultCode() == RESULT_OK && ketQua.getData() != null) {
                        Uri imageUri = ketQua.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 2;
                            bitmapAnhChup = BitmapFactory.decodeStream(inputStream, null, options);
                            if (bitmapAnhChup != null) hienThiAnhDaChon();
                        } catch (Exception e) {
                            Toast.makeText(this, "Lỗi đọc ảnh!", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void hienThiAnhDaChon() {
        if (bitmapAnhChup != null) {
            imgSelected.setVisibility(View.VISIBLE);
            imgSelected.setImageBitmap(bitmapAnhChup);
            if (layoutUploadInfo != null) layoutUploadInfo.setVisibility(View.GONE);
        }
    }

    private void setupClickEvents() {
        View.OnClickListener selectImageListener = v -> showImageSelectionDialog();
        findViewById(R.id.cardImage).setOnClickListener(selectImageListener);
        imgSelected.setOnClickListener(selectImageListener);

        btnSave.setOnClickListener(v -> {
            String noiDung = edtContent.getText().toString().trim();
            if (bitmapAnhChup == null) {
                Toast.makeText(this, "Vui lòng thêm ảnh trước!", Toast.LENGTH_SHORT).show();
                return;
            }
            if (noiDung.isEmpty()) {
                Toast.makeText(this, "Hãy viết gì đó nhé!", Toast.LENGTH_SHORT).show();
                return;
            }
            btnSave.setEnabled(false);
            btnSave.setText(isEditMode ? "Đang cập nhật..." : "Đang lưu...");
            luuDuLieuLenFirebase();
        });
    }

    private void showImageSelectionDialog() {

        String[] options = {"📸  Chụp ảnh mới", "🖼️  Chọn từ thư viện", "❌  Hủy"};

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle("Thêm ảnh vào nhật ký");

        builder.setItems(options, (dialog, which) -> {

            if (which == 0) {

                boChupAnh.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));

            } else if (which == 1) {

                boChonAnh.launch(
                        new Intent(
                                Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                        )
                );

            } else {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_5);
        }
    }

    private void kiemTraCheDoSua() {
        Intent intent = getIntent();
        if (intent.hasExtra("isEdit")) {
            isEditMode = true;
            documentId = intent.getStringExtra("documentId");
            edtContent.setText(intent.getStringExtra("oldContent"));
            String oldImage = intent.getStringExtra("oldImage");
            if (oldImage != null && !oldImage.isEmpty()) {
                byte[] b = Base64.decode(oldImage, Base64.DEFAULT);
                bitmapAnhChup = BitmapFactory.decodeByteArray(b, 0, b.length);
                hienThiAnhDaChon();
            }
            btnSave.setText("CẬP NHẬT NHẬT KÝ");
        }
    }

    private void luuDuLieuLenFirebase() {
        String text = edtContent.getText().toString().trim();
        String anhMaHoa = "";
        if (bitmapAnhChup != null) {
            // 1. Giảm kích thước ảnh xuống
            int maxWidth = 800;
            int maxHeight = (bitmapAnhChup.getHeight() * maxWidth) / bitmapAnhChup.getWidth();
            Bitmap bitmapNen = Bitmap.createScaledBitmap(bitmapAnhChup, maxWidth, maxHeight, true);
            // 2. Nén chất lượng ảnh
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmapNen.compress(Bitmap.CompressFormat.JPEG, 85, baos); // Nén chất lượng 85%
            byte[] imageBytes = baos.toByteArray();
            anhMaHoa = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        }
        Map<String, Object> nhatKy = new HashMap<>();
        nhatKy.put("noiDung", text);
        nhatKy.put("duongDanAnh", anhMaHoa);
        FirebaseFirestore firestore = FirebaseFirestore.getInstance();
        if (isEditMode && !documentId.isEmpty()) {
            firestore.collection("DanhSachNhatKy").document(documentId)
                    .update("noiDung", text, "duongDanAnh", anhMaHoa)
                    .addOnSuccessListener(unused -> {
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        quayVeMain();
                    })
                    .addOnFailureListener(e -> resetSaveButton());
        } else {
            nhatKy.put("ngayThang", new Date());
            firestore.collection("DanhSachNhatKy").add(nhatKy)
                    .addOnSuccessListener(doc -> {
                        Toast.makeText(this, "Đã lưu nhật ký!", Toast.LENGTH_SHORT).show();
                        quayVeMain();
                    })
                    .addOnFailureListener(e -> resetSaveButton());
        }
    }

    private void resetSaveButton() {
        btnSave.setEnabled(true);
        btnSave.setText(isEditMode ? "CẬP NHẬT NHẬT KÝ" : "LƯU VÀO NHẬT KÝ");
        Toast.makeText(this, "Lỗi kết nối Firebase!", Toast.LENGTH_SHORT).show();
    }

    private void quayVeMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }

}