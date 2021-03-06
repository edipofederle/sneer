package sneer.android.ui;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnKeyListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import sneer.Contact;
import sneer.Conversation;
import sneer.ConversationItem;
import sneer.Sneer;
import sneer.android.utils.AndroidUtils;
import sneer.main.R;

import static sneer.android.SneerAndroidContainer.component;
import static sneer.android.ui.ContactActivity.CURRENT_NICKNAME;
import static sneer.android.utils.Puk.shareOwnPublicKey;

public class ConversationActivity extends SneerActionBarActivity implements StartPluginDialogFragment.SingleConversationProvider {

    private static final String ACTIVITY_TITLE = "activityTitle";

    private final List<ConversationItem> messages = new ArrayList<>();
	private ConversationAdapter adapter;

	private Conversation conversation;
	private Contact contact;

	private ImageButton messageButton;
	private EditText messageInput;
	private Subscription subscription;


	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		setContentView(R.layout.activity_conversation);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);     // Attaching the layout to the toolbar object
        setSupportActionBar(toolbar);                               // Setting toolbar as the ActionBar with setSupportActionBar() call

		String nick = getIntent().getStringExtra("nick");
		contact = sneer().findByNick(nick);
		if (contact == null) {
			AndroidUtils.toast(this, "Contact not found: " + nick, Toast.LENGTH_LONG);
			finish();
			return;
		}

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);

        // TODO Register a Click Listener on the Toolbar Title via reflection. Find a better solution
        try {
            Field titleField = Toolbar.class.getDeclaredField("mTitleTextView");
            titleField.setAccessible(true);
            TextView barTitleView = (TextView) titleField.get(toolbar);
            barTitleView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onToolbarTitleClick();
                }
            });
        } catch (NoSuchFieldException | IllegalAccessException e) {
            // Ignore
        }

		plugActionBarTitle(actionBar, contact.nickname().observable());

		conversation = sneer().conversations().withContact(contact);

		adapter = new ConversationAdapter(this,
			this.getLayoutInflater(),
			R.layout.list_item_user_message,
			R.layout.list_item_party_message,
			messages,
			contact,
			conversation);

		((ListView)findViewById(R.id.messageList)).setAdapter(adapter);

		messageInput = (EditText) findViewById(R.id.editText);
		messageInput.addTextChangedListener(new TextWatcher() { @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
				messageButton.setImageResource( messageInput.getText().toString().trim().isEmpty()
					? R.drawable.ic_action_new
					: R.drawable.ic_action_send);
			}

			@Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
			@Override public void afterTextChanged(Editable s) {}
		});

		messageInput.setOnKeyListener(new OnKeyListener() { @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
			if (!isHardwareKeyboardAvailable()) return false;
			if (!(event.getAction() == KeyEvent.ACTION_DOWN)) return false;
			if (!(keyCode == KeyEvent.KEYCODE_ENTER)) return false;
			handleClick(messageInput.getText().toString().trim());
			return true;
		}});

		messageButton = (ImageButton)findViewById(R.id.actionButton);

		messageButton.setImageResource(R.drawable.ic_action_new);
		messageButton.setOnClickListener(new OnClickListener() { @Override public void onClick(View v) {
			handleClick(messageInput.getText().toString().trim());
		}});

		final TextView waiting = (TextView)findViewById(R.id.waitingMessage);
		final ListView messageList = (ListView)findViewById(R.id.messageList);
		String waitingMessage = this.getResources().getString(R.string.conversation_activity_waiting);
		waiting.setText(Html.fromHtml(String.format(waitingMessage, contact.nickname().current())));
		waiting.setMovementMethod(new LinkMovementMethod() { @Override public boolean onTouchEvent(@NonNull TextView widget, @NonNull Spannable buffer, @NonNull MotionEvent event) {
			if (event.getAction() == MotionEvent.ACTION_UP)
				shareOwnPublicKey(ConversationActivity.this, sneer().self(), contact.inviteCode(), contact.nickname().current());
			return true;
		}});

		conversation.canSendMessages().observeOn(AndroidSchedulers.mainThread()).subscribe(new Action1<Boolean>() {@Override public void call(Boolean canSendMessages) {
			messageInput .setEnabled(canSendMessages);
			messageButton.setEnabled(canSendMessages);
			waiting.setVisibility(canSendMessages ? View.GONE : View.VISIBLE);
			messageList.setVisibility(canSendMessages ? View.VISIBLE : View.GONE);
		}});
	}

	@Override
    public Conversation getConversation() {
        return conversation;
    }

	private void handleClick(String text) {
		if (!text.isEmpty())
			conversation.sendMessage(text);
		else
			openInteractionMenu();
		messageInput.setText("");
	}


	private void openInteractionMenu() {
        StartPluginDialogFragment startPluginDialog = new StartPluginDialogFragment();
        startPluginDialog.show(getFragmentManager(), "StartPluginDialogFrament");
	}


	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.title:
                navigateToContact();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void onToolbarTitleClick() {
        navigateToContact();
    }

    private void navigateToContact() {
		Intent intent = new Intent();
		intent.setClass(this, ContactActivity.class);
		intent.putExtra(CURRENT_NICKNAME, contact.nickname().current());
		intent.putExtra(ACTIVITY_TITLE, "Contact");
		startActivity(intent);
	}


	private void hideKeyboard() {
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(messageInput.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}


	private boolean isHardwareKeyboardAvailable() {
		return getResources().getConfiguration().keyboard != Configuration.KEYBOARD_NOKEYS;
	}

	@Override
	protected void onPause() {
		super.onPause();
		subscription.unsubscribe();
		sneer().conversations().notificationsStopIgnoring();
	}


	@Override
	protected void onResume() {
		super.onResume();
		hideKeyboard();
		sneer().conversations().notificationsStartIgnoring(conversation);
		subscription = subscribeToMessages();
	}


	private Subscription subscribeToMessages() {
		return ui(conversation.items()).subscribe(new Action1<List<ConversationItem>>() {
			@Override
			public void call(List<ConversationItem> msgs) {
			messages.clear();
			messages.addAll(msgs);
			adapter.notifyDataSetChanged();
			ConversationItem last = lastMessageReceived(msgs);
			if (last != null)
				conversation.setRead(last);
			}
		});
	}


	private ConversationItem lastMessageReceived(List<ConversationItem> ms) {
		for (int i = ms.size() - 1; i >= 0; --i) {
			ConversationItem message = ms.get(i);
			if (!message.isOwn())
				return message;
		}
		return null;
	}

	private static Sneer sneer() {
		return component(Sneer.class);
	}


}
