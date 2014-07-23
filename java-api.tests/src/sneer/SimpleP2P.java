package sneer;

import static sneer.ObservableTestUtils.*;
import static sneer.tuples.TupleUtils.*;

import java.io.*;

import org.junit.*;

import sneer.impl.keys.*;
import sneer.tuples.*;

public class SimpleP2P extends TestsBase {
	
	
	@Test
	public void messagePassing() throws IOException {

		TuplePublisher publisher = tuplesA.newTuplePublisher()
			.audience(userB.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper");
			
		publisher.pub("rock");
		
		publisher.type("rock-paper-scissor/message")
			.pub("hehehe");
		
		
		TupleSubscriber subscriber = tuplesB.newTupleSubscriber();

		expectValues(subscriber.tuples(), "paper", "rock", "hehehe");
		expectValues(subscriber.type("rock-paper-scissor/move").tuples(), "paper", "rock");
		expectValues(subscriber.type("rock-paper-scissor/message").tuples(), "hehehe");
		
	}

	@Test
	public void tupleWithType() throws IOException {

		tuplesA.newTuplePublisher()
			.audience(userB.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper")
			.type("rock-paper-scissor/message")
			.pub("hehehe");
		
		assertEqualsUntilNow(tuplesB.newTupleSubscriber().tuples().map(TO_TYPE), "rock-paper-scissor/move", "rock-paper-scissor/message");
		
	}
	
	@Test
	public void targetUser() {
		
		tuplesA.newTuplePublisher()
			.audience(userC.publicKey())
			.type("rock-paper-scissor/move")
			.pub("paper");
		
		assertCount(0, tuplesB.newTupleSubscriber().tuples());
		assertCount(1, tuplesC.newTupleSubscriber().tuples());
	}
	
	@Test
	public void publicTuples() {
		
		tuplesA.newTuplePublisher()
			.type("profile/name")
			.pub("UserA McCloud");
		
		assertCount(1, tuplesA.newTupleSubscriber().tuples()); // should I receive my own public tuples?
		assertCount(1, tuplesB.newTupleSubscriber().tuples());
		assertCount(1, tuplesC.newTupleSubscriber().tuples());
		
	}
	
	@Test
	public void byAuthor() {
		tuplesA.newTuplePublisher()
			.type("profile/name")
			.pub("UserA McCloud");
		
		assertCount(1, tuplesB.newTupleSubscriber().author(userA.publicKey()).tuples());
		assertCount(0, tuplesB.newTupleSubscriber().author(userC.publicKey()).tuples());
	}
	
	@Test
	public void audienceIgnoresPublic() {
		
		tuplesA.newTuplePublisher()
			.type("chat/message")
			.pub("hey people!");
		
		PrivateKey group = Keys.createPrivateKey();
		assertCount(0, tuplesB.newTupleSubscriber().audience(group).tuples());
	}
	
//	public static void main(String[] args) {
//		
//		Sneer sneer = SneerFactory.newSneer();
//		
//		KeyPair fabio = sneer.newKeyPair();
//		PublicKey felipePuk = null;
//		PublicKey diegoPuk = null;
//		
//		Cloud cloud = sneer.newCloud(fabio);
//		
//		
//		
//		cloud.newTuplePublisher()
//			.type("profile", "name")
//			.value("Fabio Roger Manera")
//			.pub();
//		
//		
//		//   /:me/contacts/felipePuk/nickname
//		
//		cloud.newTuplePublisher()
//			.audience(sneer.self())
//			.type("contact", felipePuk, "nickname")
//			.pub("Felipe");
//		
//		
//		Map<String, Object> map = new HashMap<String, Object>();
//		map.put("audience", sneer.self());
//		map.put("type", "contact");
//		map.put("field", "nickname");
//		map.put("puk", felipePuk);
//		map.put("value", "Felipe");
//		
//		
//		
//		
//		
//		
//		TuplePublisher moves = cloud.newTuplePublisher()
//			.audience(felipePuk)
//			.type("rock-paper-scissor", "move");
//		
//		moves.pub("rock");
//		moves.pub("paper");
//	
//		TuplePublisher messages = cloud.newTuplePublisher()
//			.type("rock-paper-scissor", "message");
//		
//		messages.audience(felipePuk).pub("opa!!");
//		messages.audience(diegoPuk).pub("opa, beleza??");
//		
//		
//		
//	
//		
//		
//		Cloud felipeCloud = null;
//		
//		felipeCloud.newTupleSubscriber()
//			.type("rock-paper-scissor")
//			.tuples()
//			.subscribe(new Action1<Tuple>() {  @Override public void call(Tuple t1) {
//				System.out.println("----> " + t1);
//			}});
//	
//		felipeCloud.newTupleSubscriber()
//			.type("rock-paper-scissor", "message")
//			.tuples()
//			.map(new Func1<Tuple, String>() {  @Override public String call(Tuple t1) {
//				return (String) t1.value(); 
//			} })
//			.subscribe(new Action1<String>() {  @Override public void call(String t1) {
//				System.out.println("----> " + t1);
//			}});
//	
//	
//	
//	
//	Tuple nickname = sneer.cloud().newTupleSubscriber()
//			.audience(sneer.self())
//			.author(sneer.self())
//			.type("nickname")
//			.where("puk", tuple.author())
//			.localOrNull()
//			.values()
//			.toBlockingObservable()
//			.last();
////			.values()
////			.cast(String.class)
////			.subscribe(new Action1<String>() {  @Override public void call(String nickname) {
////				System.out.println("-----> " + nickname);
////			}});
//
//
//		sneer.cloud().newTupleSubscriber()
//			.audience(sneer.self())
//			.author(sneer.self())
//			.type("picture")
//			.where("puk", tuple.author())
//			.values()
//			.cast(String.class)
//			.subscribe(new Action1<String>() {  @Override public void call(String nickname) {
//				System.out.println("-----> " + nickname);
//			}});
//
//		sneer.ownFile("notes", tuple.author())
//			.subscribe(new Action1<DataInput>() {  @Override public void call(DataInput in) {
//				// use in
//			}});
//		
//		sneer.cloud().newTupleSubscriber()
//			.audience(sneer.self())
//			.author(sneer.self())
//			.type("file")
//			.where("path", new Object[]{"notes", tuple.author()})
//			.values()
//			.cast(byte[].class)
//			.subscribe(new Action1<byte[]>() {  @Override public void call(byte[] notes) {
//				DataInput in = new DataInputStream(new ByteArrayInputStream(notes));
//				// use in
//			}});
//
//
//		sneer.cloud().newTupleSubscriber()
//			.author(sneer.self())
//			.audience(sneer.self())
//			.nested("contact")
//				.nested(tuple.author())
//					.nested("nickname")
//						.currentValue();

	
//	}
	
	
	
}
