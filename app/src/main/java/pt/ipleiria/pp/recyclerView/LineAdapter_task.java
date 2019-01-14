package pt.ipleiria.pp.recyclerView;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;

import pt.ipleiria.pp.GameActivity;
import pt.ipleiria.pp.R;
import pt.ipleiria.pp.TaskActivity;
import pt.ipleiria.pp.model.Game;
import pt.ipleiria.pp.model.Task;

public class LineAdapter_task extends RecyclerView.Adapter<LineHolder_task> {

    public static final String ID_VIEW_TASK = "id_viewTask";
    private ArrayList<Task> mTaks;

    public LineAdapter_task(ArrayList taks) {
        this.mTaks = taks;
    }

    @NonNull
    @Override
    public LineHolder_task onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new LineHolder_task(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_task_view, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull LineHolder_task lineHolder_taks, int i) {
        final int position = i;
        lineHolder_taks.taskTitle.setText(mTaks.get(i).getTitle());
        lineHolder_taks.taskDescription.setText(mTaks.get(i).getDescription());
        lineHolder_taks.taskOrder.setText("" + mTaks.get(i).getOrder());
        lineHolder_taks.checkBox.setVisibility(View.INVISIBLE);

        if(mTaks.get(i).isTaskComplete()) {
            lineHolder_taks.checkBox.setChecked(mTaks.get(i).isTaskComplete());
            lineHolder_taks.checkBox.setVisibility(View.VISIBLE);
        }

        lineHolder_taks.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (!mTaks.get(position).isTaskComplete()){

                    Intent intent = new Intent(v.getContext(), TaskActivity.class);
                    intent.putExtra(ID_VIEW_TASK, mTaks.get(position).getId());
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                    v.getContext().startActivity(intent);
                }else {
                    Toast.makeText(v.getContext(), "A Tarefa ja foi concluida!", Toast.LENGTH_LONG).show();
                }



            }
        });
    }

    @Override
    public int getItemCount() {
        return mTaks != null ? mTaks.size() : 0;
    }

    public void updateList(Task task) {
        insertItem(task);
    }

    private void insertItem(Task task) {
        mTaks.add(task);
        notifyItemInserted(getItemCount());
    }

    // Método responsável por atualizar um usuário já existente na lista.
    private void updateItem(int position) {
        Task task = mTaks.get(position);
        notifyItemChanged(position);
    }

    // Método responsável por remover um usuário da lista.
    public void removerItem(int position) {
        mTaks.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mTaks.size());
        int i;
        if (position == 0) {
            i = position;
        } else {
            i = position - 1;
        }
        for (; i < mTaks.size(); i++) {
            mTaks.get(i).setOrder(i + 1);
        }
        notifyDataSetChanged();
    }

    public void updateFullList(Game game) {
        mTaks = game.getTasks();
        notifyDataSetChanged();
    }

    public String EditItem(int position) {
        String id = mTaks.get(position).getId();
        notifyItemChanged(position);
        return id;
    }


    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mTaks, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mTaks, i, i - 1);
            }
        }
        for (int i = 0; i < mTaks.size(); i++) {
            mTaks.get(i).setOrder(i + 1);
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }
}
