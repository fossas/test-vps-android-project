package com.hapramp.ui.activity;

import android.Manifest;
import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.ProgressDialog;
import android.app.TimePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.google.gson.Gson;
import com.hapramp.R;
import com.hapramp.api.RetrofitServiceGenerator;
import com.hapramp.datastore.UrlBuilder;
import com.hapramp.draft.ContestDraftModel;
import com.hapramp.draft.DraftsHelper;
import com.hapramp.models.CompetitionCreateResponse;
import com.hapramp.models.FormattedBodyResponse;
import com.hapramp.models.JudgeModel;
import com.hapramp.models.error.ErrorResponse;
import com.hapramp.models.requests.CompetitionCreateBody;
import com.hapramp.preferences.HaprampPreferenceManager;
import com.hapramp.steem.PermlinkGenerator;
import com.hapramp.steem.SteemPostCreator;
import com.hapramp.utils.CommunityUtils;
import com.hapramp.utils.ErrorUtils;
import com.hapramp.utils.GoogleImageFilePathReader;
import com.hapramp.utils.MomentsUtils;
import com.hapramp.utils.PostHashTagPreprocessor;
import com.hapramp.views.JudgeSelectionView;
import com.hapramp.views.hashtag.CustomHashTagInput;
import com.hapramp.views.post.PostCommunityView;
import com.hapramp.views.post.PostImageView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.hapramp.ui.activity.JudgeSelectionActivity.EXTRA_SELECTED_JUDGES;

public class CompetitionCreatorActivity extends AppCompatActivity implements JudgeSelectionView.JudgeSelectionCallback, SteemPostCreator.SteemPostCreatorCallback, DraftsHelper.DraftsHelperCallback {
  public static final long NO_COMP_DRAFT = -1;
  public static final String EXTRA_KEY_DRAFT_ID = "draftId";
  public static final String EXTRA_KEY_DRAFT_JSON = "draftJson";
  private static final int REQUEST_IMAGE_SELECTOR = 1039;
  private static final int REQUEST_JUDGE_SELECTOR = 1037;
  private static final String REGISTER_PERMLINK_ANNOUNCE_TYPE = "announce";
  @BindView(R.id.backBtn)
  ImageView backBtn;
  @BindView(R.id.toolbar_title)
  TextView toolbarTitle;
  @BindView(R.id.nextButton)
  TextView nextButton;
  @BindView(R.id.toolbar_container)
  RelativeLayout toolbarContainer;
  @BindView(R.id.competition_title)
  EditText competitionTitle;
  @BindView(R.id.competition_description)
  EditText competitionDescription;
  @BindView(R.id.competition_rules)
  EditText competitionRules;
  @BindView(R.id.backBtnFromCompetionMeta)
  ImageView backBtnFromCompetionMeta;
  @BindView(R.id.publishButton)
  TextView publishButton;
  @BindView(R.id.meta_toolbar_container)
  RelativeLayout metaToolbarContainer;
  @BindView(R.id.community_caption)
  TextView communityCaption;
  @BindView(R.id.competitionCommunityView)
  PostCommunityView competitionCommunityView;
  @BindView(R.id.tagsCaption)
  TextView tagsCaption;
  @BindView(R.id.tagsInputBox)
  CustomHashTagInput tagsInputBox;
  @BindView(R.id.timingCaption)
  TextView timingCaption;
  @BindView(R.id.starts_on_caption)
  TextView startsOnCaption;
  @BindView(R.id.start_time_input)
  EditText startTimeInput;
  @BindView(R.id.start_clock_icon)
  ImageView startClockIcon;
  @BindView(R.id.start_date_input)
  EditText startDateInput;
  @BindView(R.id.start_date_icon)
  ImageView startDateIcon;
  @BindView(R.id.start_timing_container)
  RelativeLayout startTimingContainer;
  @BindView(R.id.ends_on_caption)
  TextView endsOnCaption;
  @BindView(R.id.end_time_input)
  EditText endTimeInput;
  @BindView(R.id.end_clock_icon)
  ImageView endClockIcon;
  @BindView(R.id.end_date_input)
  EditText endDateInput;
  @BindView(R.id.end_date_icon)
  ImageView endDateIcon;
  @BindView(R.id.end_timing_container)
  RelativeLayout endTimingContainer;
  @BindView(R.id.prizeCaption)
  TextView prizeCaption;
  @BindView(R.id.first_prize_caption)
  TextView firstPrizeCaption;
  @BindView(R.id.first_prize_input)
  EditText firstPrizeInput;
  @BindView(R.id.second_prize_caption)
  TextView secondPrizeCaption;
  @BindView(R.id.second_prize_input)
  EditText secondPrizeInput;
  @BindView(R.id.third_prize_caption)
  TextView thirdPrizeCaption;
  @BindView(R.id.third_prize_input)
  EditText thirdPrizeInput;
  @BindView(R.id.prizes_container)
  RelativeLayout prizesContainer;
  @BindView(R.id.bannerCaption)
  TextView bannerCaption;
  @BindView(R.id.choose_banner_image_btn)
  TextView chooseBannerImageButton;
  @BindView(R.id.competition_banner)
  PostImageView competitionBanner;
  @BindView(R.id.skills_wrapper)
  RelativeLayout skillsWrapper;
  @BindView(R.id.metaView)
  RelativeLayout metaView;
  @BindView(R.id.judge_selector)
  JudgeSelectionView judgeSelector;
  private boolean isBannerSelected;
  private String bannerImageDownloadUrl = "";
  private ArrayList<JudgeModel> selectedJudges;
  private ProgressDialog progressDialog;
  private SteemPostCreator steemPostCreator;
  private boolean isCompetitionPosted;
  private long mDraftId = NO_COMP_DRAFT;
  private DraftsHelper mDraftHelper;
  private CompetitionCreateBody competitionCreateBody;
  private boolean shouldSaveOrUpdateDraft = true;
  private boolean leftActivityWithPurpose = false;
  private String contestAnnouncementSteemPostPermlink;
  private String contestId = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_competition_editor);
    ButterKnife.bind(this);
    initialize();
    attachListeners();
  }

  private void initialize() {
    hideKeyboardByDefault();
    mDraftHelper = new DraftsHelper();
    mDraftHelper.setDraftsHelperCallback(this);
    progressDialog = new ProgressDialog(this);
    selectedJudges = new ArrayList<>();
    competitionCommunityView.initCategory();
    competitionBanner.setImageActionListener(new PostImageView.ImageActionListener() {
      @Override
      public void onImageRemoved() {
        isBannerSelected = false;
        bannerImageDownloadUrl = "";
      }

      @Override
      public void onImageUploaded(String downloadUrl) {
        bannerImageDownloadUrl = downloadUrl;
      }
    });
    collectExtras();
  }

  private void attachListeners() {
    nextButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showMetaView(true);
      }
    });
    backBtnFromCompetionMeta.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showMetaView(false);
      }
    });

    backBtn.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showExistAlert();
      }
    });

    publishButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if (validateFields()) {
          prepareCompetition();
        }
      }
    });

    startTimeInput.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showTimePicker("Select competition start time", startTimeInput);
      }
    });
    startClockIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showTimePicker("Select competition start time", startTimeInput);
      }
    });

    startDateInput.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showDatePicker("Select competition start date", startDateInput);
      }
    });
    startDateIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showDatePicker("Select competition start date", startDateInput);
      }
    });

    endTimeInput.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showTimePicker("Select competition end time", endTimeInput);
      }
    });
    endClockIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showTimePicker("Select competition end time", endTimeInput);
      }
    });

    endDateInput.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showDatePicker("Select competition end date", endDateInput);
      }
    });
    endDateIcon.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        showDatePicker("Select competition end date", endDateInput);
      }
    });

    chooseBannerImageButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        openGallery();
      }
    });

    judgeSelector.setJudgeSelectionCallback(this);

  }

  private void hideKeyboardByDefault() {
    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
  }

  private void collectExtras() {
    Intent receiveIntent = getIntent();
    if (receiveIntent != null) {
      mDraftId = receiveIntent.getLongExtra(EXTRA_KEY_DRAFT_ID, NO_COMP_DRAFT);
      if (mDraftId != NO_COMP_DRAFT) {
        String draftJson = receiveIntent.getStringExtra(EXTRA_KEY_DRAFT_JSON);
        try {
          ContestDraftModel contestDraftModel = new Gson().fromJson(draftJson, ContestDraftModel.class);
          loadDraft(contestDraftModel);
        }
        catch (Exception e) {
          e.printStackTrace();
          mDraftId = NO_COMP_DRAFT;
        }
      }
    }
  }

  private void showMetaView(boolean show) {
    if (metaView != null) {
      if (show) {
        metaView.setVisibility(View.VISIBLE);
      } else {
        metaView.setVisibility(View.GONE);
      }
    }
  }

  private void showExistAlert() {
    //if there is already draft, show a progress with saving...
    if (mDraftId != NO_COMP_DRAFT) {
      showPublishingProgressDialog(true, "Saving changes...");
      shouldSaveOrUpdateDraft = false;
      updateDraft();
      closeAfterSomeTime();
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this)
      .setTitle("Save as draft?")
      .setMessage("You can edit and publish saved drafts later.")
      .setPositiveButton("Save Draft", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          //save or update draft
          shouldSaveOrUpdateDraft = true;
          close();
        }
      })
      .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          // in delete mode
          shouldSaveOrUpdateDraft = false;
          close();
        }
      });
    builder.show();
  }

  private boolean validateFields() {
    if (competitionTitle.getText().toString().trim().length() == 0) {
      toast("Title required!");
      return false;
    }
    if (competitionDescription.getText().toString().trim().length() == 0) {
      toast("Competition description required!");
      return false;
    }
    if (competitionCommunityView.getSelectedTags().size() <= 1) {
      toast("Atleast 1 community required!");
      return false;
    }

    if (startTimeInput.getText().length() == 0) {
      toast("Select competition start time!");
      return false;
    }

    if (startDateInput.getText().length() == 0) {
      toast("Select competition start date!");
      return false;
    }

    if (endTimeInput.getText().length() == 0) {
      toast("Select competition end time!");
      return false;
    }

    if (endDateInput.getText().length() == 0) {
      toast("Select competition end date!");
      return false;
    }

    if (firstPrizeInput.getText().toString().trim().length() == 0) {
      toast("First prize is required!");
      return false;
    }

    if (isBannerSelected) {
      if (bannerImageDownloadUrl.length() == 0) {
        toast("Still uploading banner image. Please wait...");
        return false;
      }
    } else {
      toast("Select competition banner image.");
      return false;
    }

    if (selectedJudges.size() == 0) {
      toast("Select Atleast 1 Judge.");
      return false;
    }

    return true;
  }

  private void prepareCompetition() {
    showPublishingProgressDialog(true, "Publishing your contest...");
    competitionCreateBody = new CompetitionCreateBody();
    competitionCreateBody.setmImage(bannerImageDownloadUrl);
    competitionCreateBody.setmTitle(competitionTitle.getText().toString().trim());
    competitionCreateBody.setmDescription(competitionDescription.getText().toString().trim());
    competitionCreateBody.setmStartsAt(getStartTime());
    competitionCreateBody.setmEndsAt(getEndTime());
    competitionCreateBody.setmRules(competitionRules.getText().toString());
    competitionCreateBody.setmJudges(getSelectedJudgesIds());
    competitionCreateBody.setmCommunities(getSelectedCommunityIds());
    competitionCreateBody.setmPrizes(getPrizes());
    createCompetition(competitionCreateBody);
  }

  private void showTimePicker(String msg, final EditText targetInput) {
    Calendar mcurrentTime = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
    int hour = mcurrentTime.get(Calendar.HOUR_OF_DAY);
    int minute = mcurrentTime.get(Calendar.MINUTE);
    TimePickerDialog mTimePicker;
    mTimePicker = new TimePickerDialog(CompetitionCreatorActivity.this, new TimePickerDialog.OnTimeSetListener() {
      @Override
      public void onTimeSet(TimePicker timePicker, int selectedHour, int selectedMinute) {
        if (targetInput != null) {
          targetInput.setText(String.format(Locale.US, "%02d:%02d:00.000Z", selectedHour, selectedMinute));
        }
      }
    }, hour, minute, true);//Yes 24 hour time
    mTimePicker.setTitle(msg);
    mTimePicker.show();
  }

  private void showDatePicker(String msg, final EditText targetInput) {
    final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT+0"));
    int mYear = c.get(Calendar.YEAR);
    int mMonth = c.get(Calendar.MONTH);
    int mDay = c.get(Calendar.DAY_OF_MONTH);
    DatePickerDialog datePickerDialog = new DatePickerDialog(this,
      new DatePickerDialog.OnDateSetListener() {

        @Override
        public void onDateSet(DatePicker view, int year,
                              int monthOfYear, int dayOfMonth) {
          if (targetInput != null) {
            targetInput.setText(String.format(Locale.US, "%02d-%02d-%02d", year, 1 + monthOfYear, dayOfMonth));
          }
        }
      }, mYear, mMonth, mDay);
    datePickerDialog.setMessage(msg);
    datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis());
    datePickerDialog.show();
  }

  private void openGallery() {
    leftActivityWithPurpose = true;
    try {
      if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ||
        ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_IMAGE_SELECTOR);
      } else {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_SELECTOR);
      }
    }
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void loadDraft(ContestDraftModel draft) {
    selectedJudges = (ArrayList<JudgeModel>) draft.getJudges();
    competitionTitle.setText(draft.getCompetitionTitle());
    competitionDescription.setText(draft.getCompetitionDescription());
    competitionRules.setText(draft.getCompetitionRules());
    competitionCommunityView.setDefaultSelection(draft.getmCommunitySelection());
    competitionCommunityView.initCategory();
    tagsInputBox.setDefaultHashTags((ArrayList<String>) draft.getCustomHashTags());
    judgeSelector.setJudgesList(selectedJudges);
    startTimeInput.setText(draft.getStartTime());
    startDateInput.setText(draft.getStartDate());
    endTimeInput.setText(draft.getEndTime());
    endDateInput.setText(draft.getEndDate());
    firstPrizeInput.setText(draft.getFirstPrize());
    String downloadUrl = draft.getCompetitionPosterDownloadUrl();
    if (downloadUrl != null) {
      bannerImageDownloadUrl = draft.getCompetitionPosterDownloadUrl();
      competitionBanner.setDownloadUrl(bannerImageDownloadUrl);
      isBannerSelected = true;
    }
  }

  private void showPublishingProgressDialog(boolean show, String msg) {
    if (progressDialog != null) {
      if (show) {
        progressDialog.setMessage(msg);
        progressDialog.setCancelable(false);
        progressDialog.setIndeterminate(true);
        progressDialog.show();
      } else {
        progressDialog.dismiss();
      }
    }
  }

  private void updateDraft() {
    ContestDraftModel competitionDraftModel = new ContestDraftModel();
    competitionDraftModel.setDraftId(mDraftId);
    competitionDraftModel.setCompetitionTitle(competitionTitle.getText().toString());
    competitionDraftModel.setCompetitionDescription(competitionDescription.getText().toString());
    competitionDraftModel.setCompetitionRules(competitionRules.getText().toString());
    competitionDraftModel.setmCommunitySelection(competitionCommunityView.getSelectedTags());
    competitionDraftModel.setCustomHashTags(tagsInputBox.getHashTags());
    competitionDraftModel.setJudges(selectedJudges);
    competitionDraftModel.setStartDate(startDateInput.getText().toString());
    competitionDraftModel.setStartTime(startTimeInput.getText().toString());
    competitionDraftModel.setEndDate(endDateInput.getText().toString());
    competitionDraftModel.setEndTime(endTimeInput.getText().toString());
    competitionDraftModel.setFirstPrize(firstPrizeInput.getText().toString());
    competitionDraftModel.setCompetitionPosterDownloadUrl(competitionBanner.getDownloadUrl());
    mDraftHelper.updateContestDraft(competitionDraftModel);
  }

  private void closeAfterSomeTime() {
    new Handler().postDelayed(new Runnable() {
      @Override
      public void run() {
        showPublishingProgressDialog(false, "");
        close();
      }
    }, 2000);
  }

  private void close() {
    finish();
    overridePendingTransition(R.anim.slide_down_enter, R.anim.slide_down_exit);
  }

  private void toast(String msg) {
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
  }

  private String getStartTime() {
    return String.format("%sT%s", startDateInput.getText().toString(), startTimeInput.getText().toString());
  }

  private String getEndTime() {
    return String.format("%sT%s", endDateInput.getText().toString(), endTimeInput.getText().toString());
  }

  private List<String> getSelectedJudgesIds() {
    return JudgeModel.getJudgesStringArrayListOf(selectedJudges);
  }

  private List<Integer> getSelectedCommunityIds() {
    List<String> communities = competitionCommunityView.getSelectedTags();
    List<Integer> ids = new ArrayList<>();
    for (int i = 0; i < communities.size(); i++) {
      int _id = CommunityUtils.getCommunityIdFromTitle(communities.get(i));
      if (_id >= 0) {
        int comId = CommunityUtils.getCommunityIdFromTitle(communities.get(i));
        if (!ids.contains(comId)) {
          ids.add(comId);
        }
      }
    }
    return ids;
  }

  private List<String> getPrizes() {
    List<String> prizes = new ArrayList<>();
    prizes.add(firstPrizeInput.getText().toString());
    if (secondPrizeInput.getText().length() > 0) {
      prizes.add(secondPrizeInput.getText().toString());
    }
    if (thirdPrizeInput.getText().length() > 0) {
      prizes.add(thirdPrizeInput.getText().toString());
    }
    return prizes;
  }

  /**
   * Creates competition on app server.
   *
   * @param body body of competition.
   */
  public void createCompetition(CompetitionCreateBody body) {
    String url = UrlBuilder.createCompetitionUrl();
    try {
      RetrofitServiceGenerator.getService().createCompetition(url, body).enqueue(new Callback<CompetitionCreateResponse>() {
        @Override
        public void onResponse(Call<CompetitionCreateResponse> call, Response<CompetitionCreateResponse> response) {
          if (response.isSuccessful()) {
            contestId = response.body().getCompetitionID();
            fetchFormattedBody(contestId);
          } else {
            showPublishingProgressDialog(false, "");
            ErrorResponse er = ErrorUtils.parseError(response);
            toast(er.getmMessage());
          }
        }

        @Override
        public void onFailure(Call<CompetitionCreateResponse> call, Throwable t) {
          showPublishingProgressDialog(false, "");
          toast("Failed to create competition");
        }
      });
    }
    catch (Exception e) {
      e.printStackTrace();
      showPublishingProgressDialog(false, "");
      toast("Failed to create competition");
    }
  }

  private void fetchFormattedBody(final String comp_id) {
    RetrofitServiceGenerator.getService().requestContestPostBody(comp_id).enqueue(new Callback<FormattedBodyResponse>() {
      @Override
      public void onResponse(Call<FormattedBodyResponse> call, Response<FormattedBodyResponse> response) {
        if (response.isSuccessful()) {
          createPostOnSteem(response.body().getmBody(), comp_id);
        } else {
          showPublishingProgressDialog(false, "");
          onPostCreationFailedOnSteem("Failed to prepare body of post!");
        }
      }

      @Override
      public void onFailure(Call<FormattedBodyResponse> call, Throwable t) {
        showPublishingProgressDialog(false, "");
        onPostCreationFailedOnSteem("Failed to prepare body of post!");
      }
    });
  }

  private void createPostOnSteem(String body, String mCompetitionId) {
    steemPostCreator = new SteemPostCreator();
    steemPostCreator.setSteemPostCreatorCallback(this);
    contestAnnouncementSteemPostPermlink = PermlinkGenerator.getPermlink("Contest-" + competitionCreateBody.getmTitle() + "-" + mCompetitionId);
    ArrayList<String> tags = (ArrayList<String>) competitionCommunityView.getSelectedTags();
    List<String> images = new ArrayList<>();
    //include extra hashtags
    includeExtraTags(tags);
    PostHashTagPreprocessor.processHashtags(tags);
    images.add(competitionCreateBody.getmImage());
    tags = PostHashTagPreprocessor.processHashtags(tags);
    steemPostCreator.createPost(body, competitionCreateBody.getmTitle(), images, tags, contestAnnouncementSteemPostPermlink);
  }

  private void includeExtraTags(ArrayList<String> tags) {
    tags.clear();
    tags.addAll(tagsInputBox.getHashTags());
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    leftActivityWithPurpose = false;
    if (requestCode == REQUEST_IMAGE_SELECTOR && resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
      handleImageResult(data);
    }
    if (requestCode == REQUEST_JUDGE_SELECTOR && resultCode == Activity.RESULT_OK) {
      if (data != null) {
        selectedJudges = data.getParcelableArrayListExtra(EXTRA_SELECTED_JUDGES);
        updateJudgesView();
      }
    }
  }

  private void handleImageResult(final Intent intent) {
    final Handler handler = new Handler();
    new Thread() {
      @Override
      public void run() {
        final String filePath = GoogleImageFilePathReader.getImageFilePath(CompetitionCreatorActivity.this, intent);
        handler.post(new Runnable() {
          @Override
          public void run() {
            selectImage(filePath);
          }
        });
      }
    }.start();
  }

  private void updateJudgesView() {
    judgeSelector.setJudgesList(selectedJudges);
  }

  private void selectImage(String filePath) {
    isBannerSelected = true;
    competitionBanner.setImageSource(filePath);
  }

  @Override
  public void onBackPressed() {
    showExistAlert();
  }

  @Override
  protected void onPause() {
    super.onPause();
    invalidateDraft();
  }

  private void invalidateDraft() {
    if (mDraftId != NO_COMP_DRAFT) {
      if (isCompetitionPosted) {
        //delete draft
        deleteDraft();
      } else {
        //update draft
        if (!leftActivityWithPurpose) {
          if (shouldSaveOrUpdateDraft) {
            updateDraft();
          }
        }
      }
    } else {
      if (!isCompetitionPosted && !leftActivityWithPurpose) {
        if (shouldSaveOrUpdateDraft) {
          //save draft
          addNewDraft();
        }
      }
    }
  }

  private void deleteDraft() {
    mDraftHelper.deleteDraft(mDraftId);
  }

  /**
   * Adds new draft to database.
   */
  private void addNewDraft() {
    ContestDraftModel competitionDraftModel = new ContestDraftModel();
    competitionDraftModel.setDraftId(0);
    competitionDraftModel.setCompetitionTitle(competitionTitle.getText().toString());
    competitionDraftModel.setCompetitionDescription(competitionDescription.getText().toString());
    competitionDraftModel.setCompetitionRules(competitionRules.getText().toString());
    competitionDraftModel.setmCommunitySelection(competitionCommunityView.getSelectedTags());
    competitionDraftModel.setCustomHashTags(tagsInputBox.getHashTags());
    competitionDraftModel.setJudges(selectedJudges);
    competitionDraftModel.setStartDate(startDateInput.getText().toString());
    competitionDraftModel.setStartTime(startTimeInput.getText().toString());
    competitionDraftModel.setEndDate(endDateInput.getText().toString());
    competitionDraftModel.setEndTime(endTimeInput.getText().toString());
    competitionDraftModel.setFirstPrize(firstPrizeInput.getText().toString());
    competitionDraftModel.setCompetitionPosterDownloadUrl(competitionBanner.getDownloadUrl());
    if (checkForValidSave(competitionDraftModel)) {
      mDraftHelper.saveContestDraft(competitionDraftModel);
    }
  }

  private boolean checkForValidSave(ContestDraftModel cd) {
    if (cd.getCompetitionTitle().length() > 0) {
      return true;
    }
    if (cd.getCompetitionDescription().length() > 0) {
      return true;
    }
    if (cd.getCompetitionRules().length() > 0) {
      return true;
    }
    if (cd.getmCommunitySelection().size() > 1) {
      return true;
    }
    if (cd.getCustomHashTags().size() > 0) {
      return true;
    }
    if (cd.getJudges().size() > 0) {
      return true;
    }
    if (cd.getStartTime().length() > 0 || cd.getStartDate().length() > 0 || cd.getEndDate().length() > 0 || cd.getEndTime().length() > 0) {
      return true;
    }
    if (cd.getFirstPrize().length() > 0) {
      return true;
    }
    if (cd.getCompetitionPosterDownloadUrl() != null) {
      return true;
    }
    return false;
  }

  @Override
  public void onSelectJudge() {
    leftActivityWithPurpose = true;
    Intent i = new Intent(this, JudgeSelectionActivity.class);
    i.putParcelableArrayListExtra(EXTRA_SELECTED_JUDGES, selectedJudges);
    startActivityForResult(i, REQUEST_JUDGE_SELECTOR);
  }

  @Override
  public void onPostCreatedOnSteem() {
    registerPostPermlink();
  }

  private void registerPostPermlink() {
    SingleObserver<CompetitionCreateResponse> temp = RetrofitServiceGenerator.getService().registerCompetitionPermlink(
      contestId,
      REGISTER_PERMLINK_ANNOUNCE_TYPE,
      getFullpermlink())
      .subscribeOn(Schedulers.io())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribeWith(new SingleObserver<CompetitionCreateResponse>() {
        @Override
        public void onSubscribe(Disposable d) {

        }

        @Override
        public void onSuccess(CompetitionCreateResponse competitionCreateResponse) {
          isCompetitionPosted = true;
          HaprampPreferenceManager.getInstance().setLastPostCreatedAt(MomentsUtils.getCurrentTime());
          showPublishingProgressDialog(false, "");
          toast("Contest created successfully!");
          close();
        }

        @Override
        public void onError(Throwable e) {
          e.printStackTrace();
        }
      });
  }

  private String getFullpermlink() {
    final String username = HaprampPreferenceManager.getInstance().getCurrentSteemUsername();
    return String.format("%s/%s", username, contestAnnouncementSteemPostPermlink);
  }

  @Override
  public void onPostCreationFailedOnSteem(String msg) {
    showPublishingProgressDialog(false, "");
    toast("Contest post creation failed!");
  }

  @Override
  public void onNewDraftSaved(boolean success, int draftId) {
    mDraftId = draftId;
    if (success) {
      toast("Draft Saved");
    } else {
      toast("Failed to save draft");
    }
  }

  @Override
  public void onDraftUpdated(boolean success, int draftId) {
    showPublishingProgressDialog(false, "");
  }

  @Override
  public void onDraftDeleted(boolean success) {

  }
}

