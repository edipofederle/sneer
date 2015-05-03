package sneer.conversations;

import java.util.List;

import rx.Observable;

public interface ConversationList {

	Observable<List<Summary>> summaries();

	class Summary {
		public final String party;
		public final String textPreview;
		public final String date;
		public final String unread;
		public final long id;

		public Summary(String party, String textPreview, String date, String unread, long id) {
			this.party       = party;
			this.textPreview = textPreview;
			this.date        = date;
			this.unread      = unread;
			this.id          = id;
		}
	}

	class Open {
		public final long id;

		public Open(long id) {
			this.id = id;
		}
	}

}