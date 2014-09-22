package sneer.android.main;

import static sneer.SneerAndroidClient.ERROR;
import static sneer.SneerAndroidClient.LABEL;
import static sneer.SneerAndroidClient.MESSAGE;
import static sneer.SneerAndroidClient.OWN;
import static sneer.SneerAndroidClient.PARTNER_NAME;
import static sneer.SneerAndroidClient.REPLAY_FINISHED;
import static sneer.SneerAndroidClient.RESULT_RECEIVER;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import sneer.PublicKey;
import sneer.Sneer;
import sneer.tuples.Tuple;
import sneer.tuples.TupleFilter;
import sneer.utils.SharedResultReceiver;
import sneer.utils.Value;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.ResultReceiver;

public final class PartnerSession {
	
	private final PublicKey host;
	private final PublicKey partner;
	private final long sessionId;
	private Tuple lastLocalTuple = null;
	private String tupleType;
	private ClassLoader classLoader;
	private Sneer sneer;
	private SneerPluginInfo app;
	private Context context;

	PartnerSession(SneerPluginInfo app, PublicKey host, PublicKey partner, long sessionId, Context context, Sneer sneer) {
		this.host = host;
		this.partner = partner;
		this.sessionId = sessionId;
		this.context = context;
		this.classLoader = context.getClassLoader();
		this.sneer = sneer;
		this.tupleType = app.tupleType;
		this.app = app;
	}
	
	private void sendMessage(ResultReceiver toClient, Tuple t1) {
		Bundle data = new Bundle();
		data.putString(LABEL, (String) t1.get("label"));
		data.putBoolean(OWN, t1.author().equals(sneer.self().publicKey().current()));
		data.putParcelable(MESSAGE, Value.of(t1.payload()));
		toClient.send(0, data);
	}

	private void sendReplayFinished(ResultReceiver toClient) {
		Bundle data = new Bundle();
		data.putBoolean(REPLAY_FINISHED, true);
		toClient.send(0, data);
	}
	
	private void sendError(ResultReceiver toClient, Throwable t1) {
		Bundle data = new Bundle();
		data.putString(ERROR, "Internal error ("+t1.getMessage()+")");
		toClient.send(0, data);
	}
	
	private void sendPartnerName(ResultReceiver toClient, String partnerName) {
		Bundle bundle = new Bundle();
		bundle.putString(PARTNER_NAME, partnerName);
		toClient.send(Activity.RESULT_OK, bundle);
	}

	protected SharedResultReceiver createResultReceiver() {
		return new SharedResultReceiver(new SharedResultReceiver.Callback() {  @Override public void call(Bundle resultData) {
			resultData.setClassLoader(PartnerSession.this.classLoader);
			final ResultReceiver toClient = resultData.getParcelable(RESULT_RECEIVER);
			
			if (toClient != null) {
				PartnerSession.this.setup(toClient);
			} else {
				PartnerSession.this.publish(resultData.getString(LABEL), ((Value)resultData.getParcelable(MESSAGE)).get());
			}
		} });
	}

	private void setup(final ResultReceiver toClient) {
		pipePartnerName(toClient);
		pipeMessages(toClient);
	}
	
	private void pipeMessages(final ResultReceiver toClient) {
		queryTuples().localTuples()
			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
				lastLocalTuple = t1;
				sendMessage(toClient, t1);
			}},
			new Action1<Throwable>() {  @Override public void call(Throwable t1) {
				sendError(toClient, t1);
			}},
			new Action0() {  @Override public void call() {
				sendReplayFinished(toClient);
				pipeNewTuples(toClient);
			} });
	}

	private void pipePartnerName(final ResultReceiver toClient) {
		sneer.produceParty(partner).name().subscribe(new Action1<String>() {  @Override public void call(String partnerName) {
			sendPartnerName(toClient, partnerName);
		}});
	}

	private void publish(String label, Object message) {
		sneer.tupleSpace().publisher()
			.type(tupleType)
			.audience(partner)
			.field("session", sessionId)
			.field("host", host)
			.field("label", label)
			.pub(message);
	}

	private TupleFilter queryTuples() {
		return sneer.tupleSpace().filter()
			.field("session", sessionId)
			.field("host", host)
			.type(tupleType);
	}

	private void pipeNewTuples(final ResultReceiver toClient) {
		queryTuples().tuples()
			.filter(new Func1<Tuple, Boolean>() {  @Override public Boolean call(Tuple t1) {
				if (lastLocalTuple != null) {
					if (lastLocalTuple.equals(t1)) {
						lastLocalTuple = null;
					}
					return false;
				}
				return true;
			} })
			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
				sendMessage(toClient, t1);
			} });
	}
 
	public void startActivity() {
		Intent intent = new Intent();
		intent.setClassName(app.packageName, app.activityName);
		intent.putExtra(RESULT_RECEIVER, createResultReceiver());
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		context.startActivity(intent);
	}

}