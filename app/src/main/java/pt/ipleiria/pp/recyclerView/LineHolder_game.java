package pt.ipleiria.pp.recyclerView;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import pt.ipleiria.pp.R;


public class LineHolder_game extends RecyclerView.ViewHolder {

    public TextView gameTitle, gameDescription;
    public ImageView imageView;


    public LineHolder_game(@NonNull View itemView) {
        super(itemView);

        gameTitle = (TextView) itemView.findViewById(R.id.gametitle);
        gameDescription = (TextView) itemView.findViewById(R.id.gamedescription);
        imageView = itemView.findViewById(R.id.imageView);

    }

}
