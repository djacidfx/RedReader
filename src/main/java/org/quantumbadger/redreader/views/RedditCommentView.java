/*******************************************************************************
 * This file is part of RedReader.
 *
 * RedReader is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * RedReader is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with RedReader.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/

package org.quantumbadger.redreader.views;

import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import org.quantumbadger.redreader.R;
import org.quantumbadger.redreader.account.RedditAccount;
import org.quantumbadger.redreader.account.RedditAccountManager;
import org.quantumbadger.redreader.activities.BaseActivity;
import org.quantumbadger.redreader.common.AndroidCommon;
import org.quantumbadger.redreader.common.BetterSSB;
import org.quantumbadger.redreader.common.General;
import org.quantumbadger.redreader.common.Optional;
import org.quantumbadger.redreader.common.PrefsUtility;
import org.quantumbadger.redreader.common.RRThemeAttributes;
import org.quantumbadger.redreader.common.time.TimestampUTC;
import org.quantumbadger.redreader.fragments.CommentListingFragment;
import org.quantumbadger.redreader.reddit.RedditCommentListItem;
import org.quantumbadger.redreader.reddit.api.RedditAPICommentAction;
import org.quantumbadger.redreader.reddit.kthings.RedditIdAndType;
import org.quantumbadger.redreader.reddit.prepared.RedditChangeDataManager;
import org.quantumbadger.redreader.reddit.prepared.RedditParsedComment;
import org.quantumbadger.redreader.reddit.prepared.RedditRenderableComment;

import java.util.Observer;


public class RedditCommentView extends FlingableItemView
		implements RedditChangeDataManager.Listener {

	private final AccessibilityActionManager mAccessibilityActionManager;

	private RedditCommentListItem mComment;

	private final BaseActivity mActivity;
	private final RedditChangeDataManager mChangeDataManager;
	private final RRThemeAttributes mTheme;

	private final TextView mHeader;
	private final FrameLayout mBodyHolder;

	private final IndentView mIndentView;
	private final LinearLayout mIndentedContent;

	private final float mBodyFontScale;

	private final boolean mShowLinkButtons;

	private CharSequence mHeaderText;

	private final CommentListener mListener;

	@Nullable
	private final CommentListingFragment mFragment;

	@Nullable private ActionDescriptionPair mLeftFlingAction;
	@Nullable private ActionDescriptionPair mRightFlingAction;

	@Override
	protected void onSetItemFlingPosition(final float position) {
		mIndentedContent.setTranslationX(position);
	}

	private static final class ActionDescriptionPair {
		public final RedditAPICommentAction.RedditCommentAction action;
		public final int descriptionRes;

		private ActionDescriptionPair(
				final RedditAPICommentAction.RedditCommentAction action,
				final int descriptionRes) {
			this.action = action;
			this.descriptionRes = descriptionRes;
		}
	}

	@Nullable
	private ActionDescriptionPair chooseFlingAction(final PrefsUtility.CommentFlingAction pref) {

		if(!mComment.isComment()) {
			return null;
		}

		final RedditParsedComment comment = mComment.asComment().getParsedComment();

		switch(pref) {

			case UPVOTE:
				if(mChangeDataManager.isUpvoted(comment.getIdAndType())) {
					return new ActionDescriptionPair(
							RedditAPICommentAction.RedditCommentAction.UNVOTE,
							R.string.action_vote_remove);
				} else {
					return new ActionDescriptionPair(
							RedditAPICommentAction.RedditCommentAction.UPVOTE,
							R.string.action_upvote);
				}

			case DOWNVOTE:
				if(mChangeDataManager.isDownvoted(comment.getIdAndType())) {
					return new ActionDescriptionPair(
							RedditAPICommentAction.RedditCommentAction.UNVOTE,
							R.string.action_vote_remove);
				} else {
					return new ActionDescriptionPair(
							RedditAPICommentAction.RedditCommentAction.DOWNVOTE,
							R.string.action_downvote);
				}

			case SAVE:
				if(mChangeDataManager.isSaved(comment.getIdAndType())) {
					return new ActionDescriptionPair(
							RedditAPICommentAction.RedditCommentAction.UNSAVE,
							R.string.action_unsave);
				} else {
					return new ActionDescriptionPair(
							RedditAPICommentAction.RedditCommentAction.SAVE,
							R.string.action_save);
				}

			case REPORT:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.REPORT,
						R.string.action_report
				);

			case REPLY:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.REPLY,
						R.string.action_reply);

			case CONTEXT:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.CONTEXT,
						R.string.action_comment_context
				);

			case GO_TO_COMMENT:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.GO_TO_COMMENT,
						R.string.action_comment_go_to
				);

			case COMMENT_LINKS:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.COMMENT_LINKS,
						R.string.action_comment_links
				);

			case SHARE:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.SHARE,
						R.string.action_share
				);

			case COPY_TEXT:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.COPY_TEXT,
						R.string.action_copy_text
				);

			case COPY_URL:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.COPY_URL,
						R.string.action_copy_link
				);

			case USER_PROFILE:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.USER_PROFILE,
						R.string.action_user_profile);

			case COLLAPSE:

				if(mFragment == null) {
					return null;
				}

				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.COLLAPSE,
						R.string.action_collapse);

			case ACTION_MENU:

				if(mFragment == null) {
					return null;
				}

				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.ACTION_MENU,
						R.string.action_actionmenu_short);

			case PROPERTIES:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.PROPERTIES,
						R.string.action_properties);

			case BACK:
				return new ActionDescriptionPair(
						RedditAPICommentAction.RedditCommentAction.BACK,
						R.string.action_back);

			case DISABLED:
				return null;
		}

		return null;
	}

	@NonNull
	@Override
	protected String getFlingLeftText() {

		final Context context = getContext();

		final PrefsUtility.CommentFlingAction pref =
				PrefsUtility.pref_behaviour_fling_comment_left();

		mLeftFlingAction = chooseFlingAction(pref);

		if(mLeftFlingAction == null) {
			return "Disabled";
		}

		return context.getString(mLeftFlingAction.descriptionRes);
	}

	@NonNull
	@Override
	protected String getFlingRightText() {

		final Context context = getContext();

		final PrefsUtility.CommentFlingAction pref =
				PrefsUtility.pref_behaviour_fling_comment_right();

		mRightFlingAction = chooseFlingAction(pref);

		if(mRightFlingAction == null) {
			return "Disabled";
		}

		return context.getString(mRightFlingAction.descriptionRes);
	}

	@Override
	protected boolean allowFlingingLeft() {
		return mLeftFlingAction != null;
	}

	@Override
	protected boolean allowFlingingRight() {
		return mRightFlingAction != null;
	}

	@Override
	protected void onFlungLeft() {

		if(mLeftFlingAction == null || !mComment.isComment()) {
			return;
		}

		RedditAPICommentAction.onActionMenuItemSelected(
				mComment.asComment(),
				this,
				mActivity,
				mFragment,
				mLeftFlingAction.action,
				mChangeDataManager);
	}

	@Override
	protected void onFlungRight() {

		if(mRightFlingAction == null || !mComment.isComment()) {
			return;
		}

		RedditAPICommentAction.onActionMenuItemSelected(
				mComment.asComment(),
				this,
				mActivity,
				mFragment,
				mRightFlingAction.action,
				mChangeDataManager);
	}

	public interface CommentListener {
		void onCommentClicked(RedditCommentView view);

		void onCommentLongClicked(RedditCommentView view);
	}

	public RedditCommentView(
			final BaseActivity context,
			final RRThemeAttributes themeAttributes,
			final CommentListener listener,
			@Nullable final CommentListingFragment fragment) {

		super(context);

		mAccessibilityActionManager = new AccessibilityActionManager(
				this,
				context.getResources());

		mActivity = context;
		mTheme = themeAttributes;
		mListener = listener;
		mFragment = fragment;

		mChangeDataManager = RedditChangeDataManager.getInstance(
				RedditAccountManager.getInstance(context).getDefaultAccount());

		final View rootView =
				LayoutInflater.from(context).inflate(R.layout.reddit_comment, this, true);

		mIndentView = rootView.findViewById(R.id.view_reddit_comment_indentview);
		mHeader = rootView.findViewById(R.id.view_reddit_comment_header);
		mBodyHolder = rootView.findViewById(R.id.view_reddit_comment_bodyholder);
		mIndentedContent = rootView.findViewById(R.id.view_reddit_comment_indented_content);

		final int minimumCommentHeight = PrefsUtility.pref_accessibility_min_comment_height();

		mIndentedContent.setMinimumHeight(General.dpToPixels(context, minimumCommentHeight));

		mBodyFontScale = PrefsUtility.appearance_fontscale_bodytext();
		final float mHeaderFontScale = PrefsUtility.appearance_fontscale_comment_headers();

		mHeader.setTextSize(
				TypedValue.COMPLEX_UNIT_PX,
				mHeader.getTextSize() * mHeaderFontScale);

		mShowLinkButtons = PrefsUtility.pref_appearance_linkbuttons();

		setOnClickListener(view -> mListener.onCommentClicked(this));

		setOnLongClickListener(v -> {
			mListener.onCommentLongClicked(this);
			return true;
		});
	}

	@Override
	public void onRedditDataChange(final RedditIdAndType thingIdAndType) {
		reset(mActivity, mComment, true);
	}

	public void reset(
			final BaseActivity activity,
			final RedditCommentListItem comment) {
		reset(activity, comment, false);
	}

	public void reset(
			final BaseActivity activity,
			final RedditCommentListItem comment,
			final boolean updateOnly) {

		if(!updateOnly) {
			if(!comment.isComment()) {
				throw new RuntimeException("Not a comment");
			}

			if(mComment != comment) {
				if(mComment != null) {
					mChangeDataManager.removeListener(mComment.asComment().getIdAndType(), this);
				}

				mChangeDataManager.addListener(comment.asComment().getIdAndType(), this);
			}

			mComment = comment;

			resetSwipeState();
		}

		mIndentView.setIndentation(comment.getIndent());

		final boolean hideLinkButtons = comment.asComment()
				.getParsedComment()
				.getRawComment().getAuthor().getDecoded().equalsIgnoreCase(
						"autowikibot");

		mBodyHolder.removeAllViews();
		final View commentBody = comment.asComment().getBody(
				activity,
				mTheme.rrCommentBodyCol,
				13.0f * mBodyFontScale,
				mShowLinkButtons && !hideLinkButtons);

		mBodyHolder.addView(commentBody);
		General.setLayoutMatchWidthWrapHeight(commentBody);

		((MarginLayoutParams)commentBody.getLayoutParams()).topMargin =
				General.dpToPixels(activity, 1);

		final RedditRenderableComment renderableComment = mComment.asComment();

		final int ageUnits = PrefsUtility.appearance_comment_age_units();

		final TimestampUTC postTimestamp = (mFragment != null && mFragment.getPost() != null)
				? mFragment.getPost().src.getCreatedTimeUTC()
				: null;

		final TimestampUTC parentCommentTimestamp = mComment.getParent() != null
				? mComment.getParent().asComment().getParsedComment().getRawComment()
						.getCreated_utc().getValue()
				: null;

		final boolean isCollapsed = mComment.isCollapsed(mChangeDataManager);

		final BetterSSB header = renderableComment.getHeader(
				mTheme,
				mChangeDataManager,
				activity,
				ageUnits,
				postTimestamp,
				parentCommentTimestamp);

		final Observer observer = (observable, o) -> {
			if (isCollapsed) {
				mHeaderText = "[ + ]  " + o;
			} else {
				mHeaderText = (SpannableStringBuilder) o;
			}

			AndroidCommon.runOnUiThread(() -> mHeader.setText(mHeaderText));
		};

		header.addObserver(observer);

		mHeaderText = header.get();

		mHeader.setContentDescription(renderableComment.getAccessibilityHeader(
				mTheme,
				mChangeDataManager,
				activity,
				ageUnits,
				postTimestamp,
				parentCommentTimestamp,
				isCollapsed,
				Optional.of(comment.getIndent())));

		if(isCollapsed) {
			setFlingingEnabled(false);
			//noinspection SetTextI18n
			mHeader.setText("[ + ]  "
					+ mHeaderText); // Note that this removes formatting (which is fine)
			mBodyHolder.setVisibility(GONE);

		} else {
			setFlingingEnabled(true);
			mHeader.setText(mHeaderText);
			mBodyHolder.setVisibility(VISIBLE);
		}

		setupAccessibilityActions();
	}

	private void setupAccessibilityActions() {

		final RedditAccount defaultAccount
				= RedditAccountManager.getInstance(mActivity).getDefaultAccount();
		final boolean isAuthenticated = defaultAccount.isNotAnonymous();

		mAccessibilityActionManager.removeAllActions();

		if(!mComment.isComment()) {
			return;
		}

		addAccessibilityActionFromDescriptionPair(
			chooseFlingAction(PrefsUtility.CommentFlingAction.COLLAPSE));

		mAccessibilityActionManager.addAction(R.string.button_next_comment_parent, () -> {
			mFragment.onNextParent();
		});

		mAccessibilityActionManager.addAction(R.string.button_prev_comment_parent, () -> {
			mFragment.onPreviousParent();
		});

		if (isAuthenticated) {
			addAccessibilityActionFromDescriptionPair(
					chooseFlingAction(PrefsUtility.CommentFlingAction.REPLY));
		}

		// TODO null
		if (mComment.asComment().getParsedComment().getRawComment()
				.getAuthor().getDecoded().equalsIgnoreCase(defaultAccount.username)) {

			addAccessibilityActionFromDescriptionPair(
				new ActionDescriptionPair(
					RedditAPICommentAction.RedditCommentAction.EDIT,
					R.string.action_edit));

			addAccessibilityActionFromDescriptionPair(
				new ActionDescriptionPair(
					RedditAPICommentAction.RedditCommentAction.DELETE,
					R.string.action_delete));
		}

		// #136: When "save" is implemented for comments, add an a11y action
		// here (behind an isAuthenticated guard).

		addAccessibilityActionFromDescriptionPair(
				chooseFlingAction(PrefsUtility.CommentFlingAction.USER_PROFILE));

		if (isAuthenticated) {
			addAccessibilityActionFromDescriptionPair(
					chooseFlingAction(PrefsUtility.CommentFlingAction.REPORT));
		}

		addAccessibilityActionFromDescriptionPair(
				chooseFlingAction(PrefsUtility.CommentFlingAction.SHARE));

		if (isAuthenticated) {
			addAccessibilityActionFromDescriptionPair(
				chooseFlingAction(PrefsUtility.CommentFlingAction.DOWNVOTE));

			addAccessibilityActionFromDescriptionPair(
				chooseFlingAction(PrefsUtility.CommentFlingAction.UPVOTE));
		}

		mAccessibilityActionManager.setClickHint(
			getAccessibilityHintForActionPref(PrefsUtility.pref_behaviour_actions_comment_tap())
		);

		mAccessibilityActionManager.setLongClickHint(
			getAccessibilityHintForActionPref(
					PrefsUtility.pref_behaviour_actions_comment_longclick())
		);
	}

	@Nullable
	@StringRes
	private Integer getAccessibilityHintForActionPref(
			@NonNull final PrefsUtility.CommentAction pref) {
		switch (pref) {
			case COLLAPSE:
				return R.string.action_collapse;
			case ACTION_MENU:
				return R.string.action_actionmenu;
		}
		return null;
	}

	public RedditCommentListItem getComment() {
		return mComment;
	}

	private void addAccessibilityActionFromDescriptionPair(
			@Nullable final ActionDescriptionPair pair) {

		if(pair == null) {
			return;
		}

		mAccessibilityActionManager.addAction(pair.descriptionRes, () -> {
			RedditAPICommentAction.onActionMenuItemSelected(
				mComment.asComment(),
				this,
				mActivity,
				mFragment,
				pair.action,
				mChangeDataManager
			);
		});
	}
}
