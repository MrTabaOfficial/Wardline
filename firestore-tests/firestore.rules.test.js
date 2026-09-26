"use strict";

const {readFileSync} = require("node:fs");
const path = require("node:path");
const {after, before, beforeEach, describe, test} = require("node:test");
const {
  assertFails,
  assertSucceeds,
  initializeTestEnvironment,
} = require("@firebase/rules-unit-testing");
const {
  collection, deleteField, doc, getDoc, getDocs, query, serverTimestamp, setDoc, updateDoc,
  where, writeBatch,
} = require("firebase/firestore");

const ALICE = "alice";
const BOB = "bob";
const EVE = "eve";
const CONVERSATION = `${ALICE}_${BOB}`;

let testEnv;

function verified(uid) {
  return testEnv.authenticatedContext(uid, {email: `${uid}@example.com`, email_verified: true}).firestore();
}

function unverified(uid) {
  return testEnv.authenticatedContext(uid, {email: `${uid}@example.com`, email_verified: false}).firestore();
}

function newMessage(senderId, receiverId, overrides = {}) {
  return {
    senderId,
    receiverId,
    ciphertext: "Y2lwaGVydGV4dA==",
    deleted: false,
    timestamp: serverTimestamp(),
    ...overrides,
  };
}

before(async () => {
  const [host, port] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8080").split(":");
  testEnv = await initializeTestEnvironment({
    projectId: "demo-wardline",
    firestore: {
      host,
      port: Number(port),
      rules: readFileSync(path.join(__dirname, "..", "firestore.rules"), "utf8"),
    },
  });
});

after(async () => {
  await testEnv.cleanup();
});

beforeEach(async () => {
  await testEnv.clearFirestore();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    for (const uid of [ALICE, BOB, EVE]) {
      await setDoc(doc(db, `users/${uid}`), {name: uid, email: `${uid}@example.com`, image: "aW1n"});
    }
    await setDoc(doc(db, `users/${BOB}/private/messaging`), {fcmToken: "bob-token"});
    await setDoc(doc(db, `conversations/${CONVERSATION}`), {
      participants: [ALICE, BOB],
      lastMessage: "YQ==",
      lastSenderId: ALICE,
      timestamp: new Date(),
    });
    await setDoc(doc(db, `conversations/${CONVERSATION}/messages/m1`), {
      senderId: ALICE,
      receiverId: BOB,
      ciphertext: "YQ==",
      deleted: false,
      timestamp: new Date(),
    });
  });
});

describe("users", () => {
  test("signed-out clients cannot read profiles", async () => {
    await assertFails(getDoc(doc(testEnv.unauthenticatedContext().firestore(), `users/${ALICE}`)));
  });

  test("unverified accounts cannot read profiles", async () => {
    await assertFails(getDoc(doc(unverified(EVE), `users/${ALICE}`)));
  });

  test("verified accounts can read profiles", async () => {
    await assertSucceeds(getDoc(doc(verified(EVE), `users/${ALICE}`)));
  });

  test("a new account can create only its own profile", async () => {
    const db = unverified("carol");
    await assertSucceeds(setDoc(doc(db, "users/carol"), {name: "Carol", email: "carol@example.com", image: "aW1n"}));
    await assertFails(setDoc(doc(db, "users/mallory"), {name: "M", email: "carol@example.com", image: "aW1n"}));
  });

  test("profile creation rejects extra fields and a mismatched email", async () => {
    const db = unverified("carol");
    await assertFails(setDoc(doc(db, "users/carol"),
        {name: "Carol", email: "carol@example.com", image: "aW1n", admin: true}));
    await assertFails(setDoc(doc(db, "users/carol"), {name: "Carol", email: "other@example.com", image: "aW1n"}));
  });

  test("owners can update their availability, others cannot", async () => {
    await assertSucceeds(updateDoc(doc(verified(ALICE), `users/${ALICE}`), {availability: 1}));
    await assertFails(updateDoc(doc(verified(EVE), `users/${ALICE}`), {availability: 0}));
    await assertFails(updateDoc(doc(verified(ALICE), `users/${ALICE}`), {availability: 5}));
    await assertFails(updateDoc(doc(verified(ALICE), `users/${ALICE}`), {role: "admin"}));
  });

  test("profile updates validate the changed fields only", async () => {
    await testEnv.withSecurityRulesDisabled(async (context) => {
      await updateDoc(doc(context.firestore(), `users/${ALICE}`), {image: "x".repeat(400000)});
    });
    const db = verified(ALICE);
    await assertSucceeds(updateDoc(doc(db, `users/${ALICE}`), {availability: 1}));
    await assertFails(updateDoc(doc(db, `users/${ALICE}`), {image: "y".repeat(400000)}));
    await assertFails(updateDoc(doc(db, `users/${ALICE}`), {name: ""}));
    await assertFails(updateDoc(doc(db, `users/${ALICE}`), {name: deleteField()}));
    await assertFails(updateDoc(doc(db, `users/${ALICE}`), {email: "someone-else@example.com"}));
  });

  test("unverified owners cannot update their profile", async () => {
    await assertFails(updateDoc(doc(unverified(ALICE), `users/${ALICE}`), {availability: 1}));
  });

  test("push tokens are private to their owner", async () => {
    await assertSucceeds(getDoc(doc(verified(BOB), `users/${BOB}/private/messaging`)));
    await assertFails(getDoc(doc(verified(ALICE), `users/${BOB}/private/messaging`)));
    await assertFails(setDoc(doc(verified(ALICE), `users/${BOB}/private/messaging`), {fcmToken: "stolen"}));
    await assertSucceeds(setDoc(doc(verified(ALICE), `users/${ALICE}/private/messaging`), {fcmToken: "t"}));
    await assertFails(setDoc(doc(verified(ALICE), `users/${ALICE}/private/other`), {fcmToken: "t"}));
  });
});

describe("conversations", () => {
  test("participants can list their conversations", async () => {
    const db = verified(BOB);
    await assertSucceeds(getDocs(query(collection(db, "conversations"), where("participants", "array-contains", BOB))));
  });

  test("outsiders cannot read a conversation", async () => {
    await assertFails(getDoc(doc(verified(EVE), `conversations/${CONVERSATION}`)));
  });

  test("a participant can start a conversation", async () => {
    const db = verified(EVE);
    await assertSucceeds(setDoc(doc(db, `conversations/${ALICE}_${EVE}`), {
      participants: [ALICE, EVE],
      lastMessage: "ZQ==",
      lastSenderId: EVE,
      timestamp: serverTimestamp(),
    }));
  });

  test("outsiders cannot create or hijack a conversation", async () => {
    const db = verified(EVE);
    await assertFails(setDoc(doc(db, `conversations/${CONVERSATION}`), {
      participants: [ALICE, BOB], lastMessage: "", lastSenderId: EVE, timestamp: serverTimestamp(),
    }));
    await assertFails(setDoc(doc(db, `conversations/${ALICE}_${EVE}`), {
      participants: [ALICE, BOB], lastMessage: "", lastSenderId: EVE, timestamp: serverTimestamp(),
    }));
  });

  test("the last sender can clear the preview, the other participant cannot", async () => {
    await assertFails(updateDoc(doc(verified(BOB), `conversations/${CONVERSATION}`), {lastMessage: ""}));
    await assertSucceeds(updateDoc(doc(verified(ALICE), `conversations/${CONVERSATION}`), {lastMessage: ""}));
  });

  test("lastSenderId cannot be spoofed", async () => {
    await assertFails(updateDoc(doc(verified(BOB), `conversations/${CONVERSATION}`), {
      lastMessage: "eA==", lastSenderId: ALICE, timestamp: serverTimestamp(),
    }));
  });
});

describe("messages", () => {
  const messages = (db) => collection(db, `conversations/${CONVERSATION}/messages`);

  test("participants can read messages, outsiders cannot", async () => {
    await assertSucceeds(getDocs(messages(verified(BOB))));
    await assertFails(getDocs(messages(verified(EVE))));
  });

  test("a participant can send a message together with the conversation update", async () => {
    const db = verified(BOB);
    const batch = writeBatch(db);
    batch.set(doc(messages(db)), newMessage(BOB, ALICE));
    batch.set(doc(db, `conversations/${CONVERSATION}`), {
      participants: [ALICE, BOB],
      lastMessage: "YQ==",
      lastSenderId: BOB,
      timestamp: serverTimestamp(),
    }, {merge: true});
    await assertSucceeds(batch.commit());
  });

  test("senders cannot impersonate someone else", async () => {
    await assertFails(setDoc(doc(messages(verified(BOB))), newMessage(ALICE, BOB)));
  });

  test("messages must carry a ciphertext string and not be pre-deleted", async () => {
    const db = verified(ALICE);
    await assertFails(setDoc(doc(messages(db)), newMessage(ALICE, BOB, {ciphertext: ""})));
    await assertFails(setDoc(doc(messages(db)), newMessage(ALICE, BOB, {ciphertext: {[BOB]: "Yg=="}})));
    await assertFails(setDoc(doc(messages(db)), newMessage(ALICE, BOB, {deleted: true})));
    await assertFails(setDoc(doc(messages(db)), newMessage(ALICE, BOB, {timestamp: new Date(0)})));
  });

  test("outsiders cannot post into a conversation", async () => {
    await assertFails(setDoc(doc(messages(verified(EVE))), newMessage(EVE, BOB)));
  });

  test("only the sender can delete a message, and deleting wipes the ciphertext", async () => {
    const ref = (db) => doc(db, `conversations/${CONVERSATION}/messages/m1`);
    await assertFails(updateDoc(ref(verified(BOB)), {deleted: true, ciphertext: ""}));
    await assertFails(updateDoc(ref(verified(ALICE)), {deleted: true}));
    await assertFails(updateDoc(ref(verified(ALICE)), {ciphertext: "bmV3"}));
    await assertSucceeds(updateDoc(ref(verified(ALICE)), {deleted: true, ciphertext: ""}));
  });

  test("messages cannot be hard-deleted or have fields removed", async () => {
    const ref = doc(verified(ALICE), `conversations/${CONVERSATION}/messages/m1`);
    await assertFails(updateDoc(ref, {senderId: deleteField()}));
  });
});

describe("everything else", () => {
  test("the legacy chat collection is closed", async () => {
    await assertFails(getDocs(collection(verified(ALICE), "chat")));
    await assertFails(setDoc(doc(verified(ALICE), "chat/x"), {message: "hi"}));
  });
});
