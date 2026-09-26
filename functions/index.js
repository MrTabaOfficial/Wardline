"use strict";

const {onDocumentCreated} = require("firebase-functions/v2/firestore");
const {setGlobalOptions, logger} = require("firebase-functions");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");
const {getMessaging} = require("firebase-admin/messaging");

initializeApp();
setGlobalOptions({maxInstances: 10});

const MAX_FORWARDED_CIPHERTEXT = 3000;
const STALE_TOKEN_ERRORS = new Set([
  "messaging/registration-token-not-registered",
  "messaging/invalid-registration-token",
]);

exports.notifyOnNewMessage = onDocumentCreated(
    "conversations/{conversationId}/messages/{messageId}",
    async (event) => {
      const message = event.data && event.data.data();
      if (!message) {
        return;
      }
      const {senderId, receiverId} = message;
      const participants = event.params.conversationId.split("_");
      if (typeof senderId !== "string" || typeof receiverId !== "string" ||
          !participants.includes(senderId) || !participants.includes(receiverId)) {
        logger.warn("Ignoring message with unexpected participants", event.params);
        return;
      }

      const db = getFirestore();
      const [receiver, sender, tokenDoc] = await Promise.all([
        db.doc(`users/${receiverId}`).get(),
        db.doc(`users/${senderId}`).get(),
        db.doc(`users/${receiverId}/private/messaging`).get(),
      ]);

      if (receiver.get("availability") === 1) {
        return;
      }
      const token = tokenDoc.get("fcmToken");
      if (!token) {
        return;
      }

      const data = {
        type: "chat_message",
        senderId,
        senderName: String(sender.get("name") || ""),
      };
      const ciphertext = message.ciphertext;
      if (typeof ciphertext === "string" && ciphertext.length <= MAX_FORWARDED_CIPHERTEXT) {
        data.ciphertext = ciphertext;
      }

      try {
        await getMessaging().send({token, data, android: {priority: "high"}});
      } catch (error) {
        if (STALE_TOKEN_ERRORS.has(error.code)) {
          await tokenDoc.ref.delete();
        } else {
          logger.error("Unable to send the notification", error);
        }
      }
    });
