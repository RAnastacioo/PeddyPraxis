package pt.ipleiria.pp.recyclerView;


import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


import pt.ipleiria.pp.GameActivity;
import pt.ipleiria.pp.GameOver;
import pt.ipleiria.pp.R;
import pt.ipleiria.pp.model.Game;
import pt.ipleiria.pp.model.SingletonPPB;
import pt.ipleiria.pp.win;

public class LineAdapter_game extends RecyclerView.Adapter<LineHolder_game> {

    public static final String ID_VIEW_GAME = "id_viewGame";
    public static final String GAMEOVER = "GameOver";
    private List<Game> mGames;

    public LineAdapter_game(ArrayList games) {
        this.mGames = games;
    }

    @NonNull
    @Override
    public LineHolder_game onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        return new LineHolder_game(LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.list_game_view, viewGroup, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final LineHolder_game lineHolder, int i) {
        final int position = i;
        lineHolder.gameTitle.setText(mGames.get(i).getTitle());
        lineHolder.gameDescription.setText(mGames.get(i).getDescription());

        if (mGames.get(i).isGameLost()) {
            lineHolder.imageView.setVisibility(View.VISIBLE);
            lineHolder.imageView.setImageResource(R.drawable.ic_lost);
        } else if (mGames.get(i).isGamewin()) {
            lineHolder.imageView.setVisibility(View.VISIBLE);
            lineHolder.imageView.setImageResource(R.drawable.ic_win);
        } else {
            lineHolder.imageView.setVisibility(View.INVISIBLE);
        }


        lineHolder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                    boolean alltrue=true;

                    for(int j = 0; j<mGames.get(position).getTasks().size() ; j++){
                        if(!mGames.get(position).getTasks().get(j).isTaskComplete()){
                            alltrue=false;
                        }
                    }
                    if(alltrue==true){
                        mGames.get(position).setGamewin(true);
                        mGames.get(position).setGameLost(false);
                        Toast.makeText(v.getContext(), "You win!", Toast.LENGTH_LONG).show();


                    }
                    if(alltrue!=true && mGames.get(position).isTimeOver()){
                        mGames.get(position).setGameLost(true);
                        mGames.get(position).setGamewin(false);
                        Toast.makeText(v.getContext(), "Game Over!", Toast.LENGTH_LONG).show();
                    }

                if(!mGames.get(position).isTimeOver()){

                    Intent intent = new Intent(v.getContext(), GameActivity.class);
                    intent.putExtra(ID_VIEW_GAME, mGames.get(position).getId());
                    v.getContext().startActivity(intent);

                }else{

                    Toast.makeText(v.getContext(), "Game Over!", Toast.LENGTH_LONG).show();
                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return mGames != null ? mGames.size() : 0;
    }

    public void updateFullList() {
        mGames = SingletonPPB.getInstance().getGames();
        notifyDataSetChanged();
    }

    private void insertItem(Game game) {
        mGames.add(game);
        notifyItemInserted(getItemCount());
    }

    public String EditItem(int position) {
        String id = mGames.get(position).getId();
        notifyItemChanged(position);
        return id;
    }

    // Método responsável por atualizar um usuário já existente na lista.
    private void updateItem(int position) {
        Game game = mGames.get(position);
        //game.setTitle("GAME_1");
        notifyItemChanged(position);
    }

    // Método responsável por remover um usuário da lista.
    public void removerItem(int position) {
        mGames.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, mGames.size());
    }

    public boolean onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(mGames, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(mGames, i, i - 1);

            }
        }
        notifyItemMoved(fromPosition, toPosition);
        return true;
    }

    public ArrayList<Game> searchGame(String title) {
        ArrayList<Game> res = new ArrayList<>();

        for (Game g : mGames) {
            if (g.getTitle().contains(title)) {
                res.add(g);
            }
        }

        return res;
    }
}
