package ltn.daystory;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Base64;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NhatKyAdapter extends RecyclerView.Adapter<NhatKyAdapter.NhatKyViewHolder> {

    private Context context;
    private List<NhatKy> danhSachNhatKy;

    public NhatKyAdapter(Context context, List<NhatKy> danhSachNhatKy) {
        this.context = context;
        this.danhSachNhatKy = danhSachNhatKy;
    }

    @NonNull
    @Override
    public NhatKyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_nhat_ky, parent, false);
        return new NhatKyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NhatKyViewHolder holder, int position) {
        NhatKy nhatKy = danhSachNhatKy.get(position);

        // 1. NỘI DUNG
        holder.txtNoiDung.setText(nhatKy.getNoiDung());

        // 2. XỬ LÝ NGÀY THÁNG
        try {
            Object objNgay = nhatKy.getNgayThang();
            SimpleDateFormat displayFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

            if (objNgay instanceof com.google.firebase.Timestamp) {
                Date date = ((com.google.firebase.Timestamp) objNgay).toDate();
                holder.txtNgay.setText(displayFormat.format(date));
            } else if (objNgay instanceof String) {
                holder.txtNgay.setText(objNgay.toString());
            } else {
                holder.txtNgay.setText("Vừa xong");
            }
        } catch (Exception e) {
            holder.txtNgay.setText("Ngày chưa xác định");
        }

        // 3. XỬ LÝ ẢNH
        String anhData = nhatKy.getDuongDanAnh();
        if (anhData != null && !anhData.isEmpty()) {
            try {
                byte[] decodedString = Base64.decode(anhData, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
                holder.imgNhatKy.setImageBitmap(bitmap);
            } catch (Exception e) {
                holder.imgNhatKy.setImageResource(R.drawable.ic_launcher_background);
            }
        } else {
            holder.imgNhatKy.setImageResource(R.drawable.ic_launcher_background);
        }

        // 4. CHỌN EMOJI
        holder.layoutMood.setOnClickListener(v -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            LinearLayout layout = new LinearLayout(context);
            layout.setOrientation(LinearLayout.HORIZONTAL);
            layout.setPadding(35, 28, 35, 28);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(Color.WHITE);
            bg.setCornerRadius(50f);
            layout.setBackground(bg);

            String[] emotions = {"😊", "🥰", "😭", "😴", "😡", "😎","✨"};
            final AlertDialog[] dialogHolder = new AlertDialog[1];

            for (String emoji : emotions) {
                TextView txtEmoji = new TextView(context);
                txtEmoji.setText(emoji);
                txtEmoji.setTextSize(30);
                txtEmoji.setPadding(22, 10, 22, 10);
                txtEmoji.setOnClickListener(view -> {
                    txtEmoji.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                    txtEmoji.animate().scaleX(1.5f).scaleY(1.5f).setDuration(120).withEndAction(() -> {
                        txtEmoji.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
                        holder.txtMood.setText(emoji);
                        if (dialogHolder[0] != null) dialogHolder[0].dismiss();
                    }).start();
                });
                layout.addView(txtEmoji);
            }
            builder.setView(layout);
            AlertDialog dialog = builder.create();
            dialogHolder[0] = dialog;
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            }

        });

        String strNgayHienThi = holder.txtNgay.getText().toString();

        // 5. CLICK VÀO ITEM ĐỂ XEM CHI TIẾT
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, DetailStoryActivity.class);

            // Gửi dữ liệu sang trang chi tiết
            intent.putExtra("noiDung", nhatKy.getNoiDung());
            intent.putExtra("anh", nhatKy.getDuongDanAnh());
            intent.putExtra("ngay", strNgayHienThi);

            context.startActivity(intent);
        });

        // 6. EDIT (SỬA)
        holder.btnEdit.setOnClickListener(v -> {
            Intent intent = new Intent(context, AddStoryActivity.class);
            intent.putExtra("isEdit", true);
            intent.putExtra("documentId", nhatKy.getDocumentId());
            intent.putExtra("oldContent", nhatKy.getNoiDung());
            intent.putExtra("oldImage", nhatKy.getDuongDanAnh());
            context.startActivity(intent);
        });

        // 7. DELETE (XÓA)
        holder.btnDelete.setOnClickListener(v -> {
            int currentPosition = holder.getAdapterPosition();
            if (currentPosition == RecyclerView.NO_POSITION) return;
            AlertDialog dialog = new AlertDialog.Builder(context)
                    .setTitle("Xóa nhật ký")
                    .setMessage("Bạn có chắc muốn xóa nhật ký này không?")
                    .setPositiveButton("Xóa", (d, which) -> {
                        FirebaseFirestore.getInstance()
                                .collection("DanhSachNhatKy")
                                .document(nhatKy.getDocumentId())
                                .delete()
                                .addOnSuccessListener(unused -> {
                                    Toast.makeText(context, "Đã xóa nhật ký", Toast.LENGTH_SHORT).show();
                                });
                    })
                    .setNegativeButton("Hủy", null)
                    .create();
            dialog.show();
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.bg_5);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (danhSachNhatKy != null) ? danhSachNhatKy.size() : 0;
    }

    public static class NhatKyViewHolder extends RecyclerView.ViewHolder {
        TextView txtNoiDung, txtNgay, txtMood;
        ImageView imgNhatKy, btnEdit, btnDelete;
        View layoutMood;

        public NhatKyViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNoiDung = itemView.findViewById(R.id.txtNoiDung);
            txtNgay = itemView.findViewById(R.id.txtNgay);
            txtMood = itemView.findViewById(R.id.txtMood);
            imgNhatKy = itemView.findViewById(R.id.imgNhatKy);
            btnEdit = itemView.findViewById(R.id.btnEdit);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            layoutMood = itemView.findViewById(R.id.layoutMood);
        }
    }
}