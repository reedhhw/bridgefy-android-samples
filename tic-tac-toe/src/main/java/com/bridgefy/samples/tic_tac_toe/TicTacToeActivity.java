package com.bridgefy.samples.tic_tac_toe;

import android.os.Bundle;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.bridgefy.samples.tic_tac_toe.entities.Player;

import java.util.Random;

import butterknife.BindView;
import butterknife.ButterKnife;

public abstract class TicTacToeActivity extends AppCompatActivity {

    private static final String TAG = "TicTacToeActivity";


    TableLayout mainBoard;
    TextView tv_turn;

    int[][] board;
    private int size;

    // first turn is 'X'
    char turn;
    public static char X = 'X';
    public static char O = 'O';

    // TODO bundle this logic
    char myTurnChar = X;
    boolean myTurn = true;
    boolean matchStopped = false;

    @BindView(R.id.button_new_match)
    Button btnNewMatch;

    // a Player object representing our rival
    protected Player rival;
    protected Player player;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_match);
        ButterKnife.bind(this);

        initializeTurn();
        initializeBoard();
    }

    abstract void sendMove(int[][] board);

    abstract void sendWinner();

    abstract void sendDraw(int[][] board);

    abstract void stopMatch(boolean myTurn);


    protected void initializeTurn() {
        turn = X;
        myTurnChar = X;
        myTurn = true;
    }

    protected void initializeBoard() {
        size = 3;
        board = new int[size][size];
        mainBoard = findViewById(R.id.mainBoard);
        tv_turn = findViewById(R.id.turn);

        if (myTurn) {
            tv_turn.setText(String.format(getString(R.string.your_turn),
                    String.valueOf(myTurnChar)));
        } else {
            tv_turn.setText(String.format(getString(R.string.their_turn),
                    rival.getNick(), String.valueOf(flipChar(myTurnChar))));
        }

        resetBoard(null);

        for (int i = 0; i < mainBoard.getChildCount(); i++) {
            TableRow row = (TableRow) mainBoard.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                TextView tv = (TextView) row.getChildAt(j);
                tv.setOnClickListener(MoveListener(i, j, tv));
                tv.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.black));
            }
        }
    }

    protected char flipChar(char c) {
        return c == X ? O : X;
    }

    View.OnClickListener MoveListener(final int r, final int c, final TextView tv) {
        return v -> {
            if (myTurn) {
                if (!isCellSet(r, c)) {
                    // update the logic and visual board
                    board[r][c] = turn;
                    if (turn == X) {
                        tv.setText(R.string.X);
                    } else {
                        tv.setText(R.string.O);
                    }

                    // update the turn
                    myTurn = false;

                    // get the game status
                    if (gameStatus() == 0) {
                        turn = flipChar(turn);
                        tv_turn.setText(String.format(getString(R.string.their_turn),
                                rival.getNick(), String.valueOf(flipChar(myTurnChar))));
                        sendMove(board);
                    } else if (gameStatus() == -1) {
                        tv_turn.setText(getString(R.string.draw));
                        sendDraw(board);
                        stopMatch(myTurn);
                    } else {
                        sendWinner();
                        stopMatch(myTurn);
                    }
                } else {
                    tv_turn.setText(getString(R.string.empty_square));
                }
            } else {
                tv_turn.setText(getString(R.string.wait_turn));
            }
        };
    }

    protected boolean isCellSet(int r, int c) {
        return !(board[r][c] == 0);
    }

    protected void disableInputs() {
        // disable play inputs
        for (int i = 0; i < mainBoard.getChildCount(); i++) {
            TableRow row = (TableRow) mainBoard.getChildAt(i);
            for (int j = 0; j < row.getChildCount(); j++) {
                TextView tv = (TextView) row.getChildAt(j);
                tv.setOnClickListener(null);
                tv.setTextColor(ContextCompat.getColor(getBaseContext(), R.color.gray));
            }
        }
    }

    protected void resetBoard(int[][] board) {
        for (int i = 0; i < size; i++) {
            TableRow row = (TableRow) mainBoard.getChildAt(i);
            for (int j = 0; j < size; j++) {
                char c = board != null ? (char) board[i][j] : 0;
                this.board[i][j] = c;

                TextView tv = (TextView) row.getChildAt(j);
                switch (c) {
                    case 'X':
                        tv.setText(R.string.X);
                        break;
                    case 'O':
                        tv.setText(R.string.O);
                        break;
                    default:
                        tv.setText(R.string.none);
                        break;
                }
            }
        }
    }

    private int gameStatus() {
        //0 Continue
        //1 X Wins
        //2 O Wins
        //-1 Draw

        int rowX = 0, colX = 0, rowO = 0, colO = 0;
        for (int i = 0; i < size; i++) {
            if (check_Row_Equality(i, 'X'))
                return 1;
            if (check_Column_Equality(i, 'X'))
                return 1;
            if (check_Row_Equality(i, 'O'))
                return 2;
            if (check_Column_Equality(i, 'O'))
                return 2;
            if (check_Diagonal('X'))
                return 1;
            if (check_Diagonal('O'))
                return 2;
        }

        boolean boardFull = true;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0)
                    boardFull = false;
            }
        }
        if (boardFull)
            return -1;
        else return 0;
    }

    private boolean check_Diagonal(char player) {
        int count_Equal1 = 0, count_Equal2 = 0;
        for (int i = 0; i < size; i++)
            if (board[i][i] == player)
                count_Equal1++;
        for (int i = 0; i < size; i++)
            if (board[i][size - 1 - i] == player)
                count_Equal2++;

        return (count_Equal1 == size || count_Equal2 == size);
    }

    private boolean check_Row_Equality(int r, char player) {
        int count_Equal = 0;
        for (int i = 0; i < size; i++) {
            if (board[r][i] == player)
                count_Equal++;
        }

        return (count_Equal == size);
    }

    private boolean check_Column_Equality(int c, char player) {
        int count_Equal = 0;
        for (int i = 0; i < size; i++) {
            if (board[i][c] == player)
                count_Equal++;
        }

        return (count_Equal == size);
    }


    /**
     *      METHODS FOR AUTO RESPONSE
     */
    protected int[][] makeRandomMove() {
        Log.v(TAG, "Generating random move:");
        int freeSpaces = freeSpaceCount();
        if (freeSpaces > 0) {
            int randomSquare = getRandomMove(freeSpaces);

            // fill the random square
            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    if (board[i][j] == 0)
                        randomSquare--;
                    if (randomSquare == 0)
                        board[i][j] = myTurnChar;
                }
            }
        }
        return board;
    }

    private int getRandomMove(int freeSpaces) {
        int randomMove = new Random().nextInt(freeSpaces) + 1;
        Log.v(TAG, "... Random move is: " + randomMove);
        return randomMove;
    }

    private int freeSpaceCount() {
        int freeSpaces = 0;
        for (int i = 0; i < size; i++) {
            for (int j = 0; j < size; j++) {
                if (board[i][j] == 0)
                    freeSpaces++;
            }
        }
        Log.v(TAG, "... Free spaces: " + freeSpaces);
        return freeSpaces;
    }
}
