const { onDocumentCreated } = require("firebase-functions/v2/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { initializeApp } = require("firebase-admin/app");

// Initialize Firebase Admin
initializeApp();

// --- FUNCTION 1: STUDENT -> TEACHER (SOS Alert) ---
exports.sendSOSNotification = onDocumentCreated("sesiones/{sessionId}/alertas/{alertId}", async (event) => {
    const alertData = event.data.data();
    const sessionId = event.params.sessionId;
    const db = event.data.ref.firestore;

    const sessionDoc = await db.collection("sesiones").doc(sessionId).get();

    if (!sessionDoc.exists) return;

    const teacherToken = sessionDoc.data().token_profesor;
    if (!teacherToken) return;

    const payload = {
        data: {
            title: "¡ALERTA SOS!",
            body: `${alertData.nick} ha pedido ayuda.`,
            channelId: "sos_channel",
            type: "sos_alert"
        },
        token: teacherToken,
        android: {
            priority: "high"
        }
    };

    try {
        await getMessaging().send(payload);
    } catch (error) {
        console.error("Error sending SOS:", error);
    }
});

// --- FUNCTION 2: TEACHER -> STUDENT (Individual or Multicast) ---
exports.sendTeacherMessage = onDocumentCreated("sesiones/{sessionId}/mensajes/{messageId}", async (event) => {
    const messageData = event.data.data();
    const sessionId = event.params.sessionId;
    const db = event.data.ref.firestore;

    const targetId = messageData.id_alumno;
    const messageText = messageData.texto;

    try {
        if (targetId === "todos") {
            // Multicast: Send to all students in the session
            const studentsSnap = await db.collection("sesiones").doc(sessionId).collection("alumnado").get();
            const tokens = [];

            studentsSnap.forEach(doc => {
                const token = doc.data().fcm_token;
                if (token) tokens.push(token);
            });

            if (tokens.length === 0) return;

            const multicastPayload = {
                data: {
                    title: "Aviso general",
                    body: String(messageText),
                    channelId: "teacher_channel",
                    type: "teacher_message"
                },
                tokens: tokens,
                android: { priority: "high" }
            };

            await getMessaging().sendEachForMulticast(multicastPayload);

        } else {
            // Individual: Send to a specific student
            const studentDoc = await db.collection("sesiones").doc(sessionId).collection("alumnado").doc(targetId).get();

            if (!studentDoc.exists) return;

            const studentToken = studentDoc.data().fcm_token;
            if (!studentToken) return;

            const individualPayload = {
                data: {
                    title: "Mensaje de tu profe",
                    body: String(messageText),
                    channelId: "teacher_channel",
                    type: "teacher_message"
                },
                token: studentToken,
                android: { priority: "high" }
            };

            await getMessaging().send(individualPayload);
        }
    } catch (error) {
        console.error("Error sending message:", error);
    }
});