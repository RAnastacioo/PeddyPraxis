package pt.ipleiria.pp.recyclerView;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.TextView;

import pt.ipleiria.pp.R;

class LineHolder_task extends RecyclerView.ViewHolder {

    public TextView taskTitle, taskDescription, taskOrder;
    public CheckBox checkBox;

    public LineHolder_task(@NonNull View itemView) {
        super(itemView);

        taskTitle = (TextView) itemView.findViewById(R.id.tasktitle);
        taskDescription = itemView.findViewById(R.id.taskdescription);
        taskOrder = itemView.findViewById(R.id.taskorder);
        checkBox = itemView.findViewById(R.id.checkBox);
    }
}
