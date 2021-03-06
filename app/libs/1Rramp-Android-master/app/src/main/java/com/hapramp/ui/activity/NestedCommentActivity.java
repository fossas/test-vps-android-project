package com.hapramp.ui.activity;

import android.app.ProgressDialog;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.hapramp.R;
import com.hapramp.analytics.AnalyticsParams;
import com.hapramp.analytics.EventReporter;
import com.hapramp.datastore.DataStore;
import com.hapramp.datastore.callbacks.CommentsCallback;
import com.hapramp.models.CommentModel;
import com.hapramp.preferences.HaprampPreferenceManager;
import com.hapramp.steem.SteemCommentCreator;
import com.hapramp.ui.adapters.CommentsAdapter;
import com.hapramp.utils.ImageHandler;
import com.hapramp.utils.MomentsUtils;
import com.hapramp.utils.ViewItemDecoration;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NestedCommentActivity extends AppCompatActivity implements
  SteemCommentCreator.SteemCommentCreateCallback,
  CommentsCallback {

  public static final String EXTRA_PARENT_AUTHOR = "key.parentauthor";
  public static final String EXTRA_PARENT_PERMLINK = "key.parentpermlink";
  public static final String EXTRA_TIMESTAMP = "key.timestamp";
  public static final String EXTRA_CONTENT = "key.content";
  @BindView(R.id.backBtn)
  ImageView backBtn;
  @BindView(R.id.toolbar_container)
  RelativeLayout toolbarContainer;
  @BindView(R.id.noCommentsCaption)
  TextView noCommentsCaption;
  @BindView(R.id.commentsRecyclerView)
  RecyclerView commentsRecyclerView;
  @BindView(R.id.commentLoadingProgressBar)
  ProgressBar commentLoadingProgressBar;
  @BindView(R.id.commentLoadingProgressMessage)
  TextView commentLoadingProgressMessage;
  @BindView(R.id.commentCreaterAvatar)
  ImageView commentCreaterAvatar;
  @BindView(R.id.commentInputBox)
  EditText commentInputBox;
  @BindView(R.id.sendButton)
  TextView sendButton;
  @BindView(R.id.commentInputContainer)
  RelativeLayout commentInputContainer;
  @BindView(R.id.shadow)
  ImageView shadow;

  private CommentsAdapter commentsAdapter;
  private ViewItemDecoration viewItemDecoration;
  private ProgressDialog progressDialog;
  private Typeface typeface;
  private SteemCommentCreator steemCommentCreator;
  private DataStore dataStore;
  private String parentAuthor;
  private String parentPermlink;
  private String parentTimestamp;
  private String parentContent;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_comments_list);
    ButterKnife.bind(this);
    EventReporter.addEvent(AnalyticsParams.SCREEN_NESTED_COMMENTS);
    if (getIntent() != null) {
      Bundle bundle = getIntent().getExtras();
      parentAuthor = bundle.getString(EXTRA_PARENT_AUTHOR, "");
      parentPermlink = bundle.getString(EXTRA_PARENT_PERMLINK, "");
      parentTimestamp = bundle.getString(EXTRA_TIMESTAMP, "");
      parentContent = bundle.getString(EXTRA_CONTENT, "");
      init(parentAuthor);
      attachListeners();
    }
  }


  private void init(String parentAuthor) {
    if (parentAuthor.equals(HaprampPreferenceManager.getInstance().getCurrentSteemUsername())) {
      commentInputContainer.setVisibility(View.GONE);
    }
    dataStore = new DataStore();
    steemCommentCreator = new SteemCommentCreator();
    steemCommentCreator.setSteemCommentCreateCallback(this);
    progressDialog = new ProgressDialog(this);
    commentInputBox.setText(String.format("@%s", parentAuthor));
    ImageHandler.loadCircularImage(this, commentCreaterAvatar,
      String.format(getResources().getString(R.string.steem_user_profile_pic_format),
        HaprampPreferenceManager.getInstance().getCurrentSteemUsername()));
    commentsAdapter = new CommentsAdapter(this);
    setParentToCommentAdapter();
    commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
    Drawable drawable = ContextCompat.getDrawable(this, R.drawable.comment_item_divider_view);
    viewItemDecoration = new ViewItemDecoration(drawable);
    viewItemDecoration.setWantTopOffset(false, 0);
    commentsRecyclerView.addItemDecoration(viewItemDecoration);
    commentsRecyclerView.setAdapter(commentsAdapter);
  }

  @Override
  protected void onResume() {
    super.onResume();
    dataStore.requestComments(parentAuthor,parentPermlink,this);
  }
  private void setParentToCommentAdapter(){
    commentsAdapter.setParentData(parentAuthor, parentTimestamp, parentContent);
  }
  private void setCommentsToAdapter(List<CommentModel> commentModel){
    setParentToCommentAdapter();
    commentsAdapter.addComments(commentModel);
  }

  private void attachListeners() {
    sendButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        postComment();
      }
    });
    backBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        finish();
      }
    });
    commentInputBox.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        if (charSequence.toString().trim().length() > 0) {
          sendButton.setTextColor(Color.parseColor("#ff6b95"));
          sendButton.setEnabled(true);
        } else {
          sendButton.setTextColor(Color.parseColor("#8eff6b95"));
          sendButton.setEnabled(false);
        }
      }

      @Override
      public void afterTextChanged(Editable editable) {

      }
    });
  }

  private void postComment() {
    String cmnt = commentInputBox.getText().toString().trim();
    commentInputBox.setText("");
    if (cmnt.length() > 2) {
      steemCommentCreator.createComment(cmnt, parentAuthor, parentPermlink);
    } else {
      Toast.makeText(this, "Comment Too Short!!", Toast.LENGTH_LONG).show();
      return;
    }
    //add temp comment to view
    CommentModel commentModel = new CommentModel();
    commentModel.setAuthor(HaprampPreferenceManager.getInstance().getCurrentSteemUsername());
    commentModel.setBody(cmnt);
    commentModel.setCreatedAt(MomentsUtils.getCurrentTime());
    commentsAdapter.addSingleComment(commentModel);
  }

  @Override
  public void onCommentCreateProcessing() {
  }

  @Override
  public void onCommentCreated() {
    hideProgress();
  }

  @Override
  public void onCommentCreateFailed() {
    hideProgress();
  }

  private void hideProgress() {
    if (progressDialog != null) {
      progressDialog.dismiss();
    }
  }

  private void hideProgressInfo() {
    if (commentLoadingProgressBar != null) {
      commentLoadingProgressBar.setVisibility(View.GONE);
      commentLoadingProgressMessage.setVisibility(View.GONE);
    }
  }

  @Override
  public void onCommentsFetching() {
    if (commentLoadingProgressBar != null) {
      commentLoadingProgressBar.setVisibility(View.VISIBLE);
      commentLoadingProgressMessage.setVisibility(View.VISIBLE);
    }
  }

  @Override
  public void onCommentsAvailable(ArrayList<CommentModel> comments) {
    setCommentsToAdapter(comments);
    hideProgressInfo();
  }

  @Override
  public void onCommentsFetchError(String error) {
    if (commentLoadingProgressBar != null) {
      commentLoadingProgressBar.setVisibility(View.GONE);
      commentLoadingProgressMessage.setVisibility(View.VISIBLE);
      commentLoadingProgressMessage.setText(R.string.comment_fetch_error_text);
    }
  }
}
