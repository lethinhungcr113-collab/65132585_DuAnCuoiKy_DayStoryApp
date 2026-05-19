package ltn.daystory;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;

public class CalendarAdapter extends RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder> {

    private final ArrayList<String> daysOfMonth;
    private final ArrayList<Integer> danhSachNgayCoNote;
    private final OnItemListener onItemListener;
    private int selectedDay = -1;

    public interface OnItemListener {
        void onItemClick(String dayText);
    }

    public CalendarAdapter(ArrayList<String> daysOfMonth, ArrayList<Integer> danhSachNgayCoNote, OnItemListener onItemListener) {
        this.daysOfMonth = daysOfMonth;
        this.danhSachNgayCoNote = danhSachNgayCoNote;
        this.onItemListener = onItemListener;
    }

    // Hàm cập nhật trạng thái khi người dùng bấm chọn ngày cụ thể
    public void updateSelectedDay(int day) {
        this.selectedDay = day;
        notifyDataSetChanged(); // Vẽ lại lịch để cập nhật hiệu ứng đổi màu ngày được chọn
    }

    @NonNull
    @Override
    public CalendarViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_calendar_day, parent, false);
        return new CalendarViewHolder(view, onItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull CalendarViewHolder holder, int position) {
        String dayText = daysOfMonth.get(position);
        holder.txtDayNumber.setText(dayText);

        // Nếu là ô trống lót lịch (đầu hoặc cuối tháng)
        if (dayText.equals("")) {
            holder.imgHasDiary.setVisibility(View.GONE);
            return;
        }

        // --- LOGIC ĐỐI CHIẾU VỚI DANH SÁCH FIREBASE ĐỂ HIỆN ICON ---
        int currentDay = Integer.parseInt(dayText);

        // Kiểm tra xem số ngày này có nằm trong list danhSachNgayCoNote không
        if (danhSachNgayCoNote != null && danhSachNgayCoNote.contains(currentDay)) {
            holder.imgHasDiary.setVisibility(View.VISIBLE); // Có bài viết -> Hiện icon cuốn sổ tím
        } else {
            holder.imgHasDiary.setVisibility(View.GONE);    // Không có bài viết -> Ẩn đi
        }

        // --- XỬ LÝ ĐỔI NỀN Ô LỊCH KHI ĐƯỢC CHỌN ---
        if (currentDay == selectedDay) {
            // 1. Nếu là ngày được chọn -> Đổi nền sang background màu tím
            holder.layoutDayRoot.setBackgroundResource(R.drawable.bg_selected_day);

            // 2. Đồng thời chuyển màu chữ sang TRẮNG để nổi bật trên nền tím
            holder.txtDayNumber.setTextColor(Color.WHITE);

            // 3. Đổi icon cuốn sổ sang màu trắng
            holder.imgHasDiary.setColorFilter(Color.WHITE);
        } else {
            // Nếu KHÔNG phải ngày được chọn -> Xóa nền
            holder.layoutDayRoot.setBackgroundResource(0);

            // Trả chữ về màu tối mặc định
            holder.txtDayNumber.setTextColor(Color.parseColor("#332C54"));

            // Trả icon cuốn sổ về màu tím ban đầu
            holder.imgHasDiary.setColorFilter(Color.parseColor("#7C5AC2"));
        }
    }

    @Override
    public int getItemCount() {
        return daysOfMonth.size();
    }

    public static class CalendarViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        public final TextView txtDayNumber;
        public final LinearLayout layoutDayRoot;
        public final ImageView imgHasDiary;
        private final OnItemListener onItemListener;

        public CalendarViewHolder(@NonNull View itemView, OnItemListener onItemListener) {
            super(itemView);
            txtDayNumber = itemView.findViewById(R.id.txtDayNumber);
            imgHasDiary = itemView.findViewById(R.id.imgHasDiary);
            layoutDayRoot = itemView.findViewById(R.id.layoutDayRoot); // Thêm dòng này
            this.onItemListener = onItemListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            onItemListener.onItemClick(txtDayNumber.getText().toString());
        }
    }
}