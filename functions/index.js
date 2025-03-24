const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// Handle individual user notifications
exports.sendNotification = functions.database
    .ref('/notifications/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notification = snapshot.val();
        const token = notification.token;
        
        if (!token) {
            console.log('No token provided');
            return null;
        }

        console.log('Processing notification for token:', token.substring(0, 10) + '...');
        console.log('Notification data:', notification);

        const message = {
            notification: {
                title: notification.title,
                body: notification.description,
                clickAction: 'FLUTTER_NOTIFICATION_CLICK'
            },
            data: {
                eventId: notification.eventId,
                clubId: notification.clubId,
                clubName: notification.clubName,
                type: notification.type,
                timestamp: notification.timestamp
            },
            token: token,
            android: {
                priority: 'high',
                notification: {
                    channelId: 'unifyu_notifications',
                    priority: 'high',
                    defaultSound: true,
                    defaultVibrateTimings: true
                }
            }
        };

        try {
            const response = await admin.messaging().send(message);
            console.log('Successfully sent message:', response);
            
            // Delete the notification after sending
            await snapshot.ref.remove();
            
            return response;
        } catch (error) {
            console.error('Error sending message:', error);
            // Log the full error details
            console.error('Error code:', error.code);
            console.error('Error message:', error.message);
            console.error('Error stack:', error.stack);
            
            // If the token is invalid, remove it from the database
            if (error.code === 'messaging/invalid-registration-token') {
                const tokenRef = admin.database().ref('fcm_tokens').child(notification.userId);
                await tokenRef.remove();
                console.log('Removed invalid token for user:', notification.userId);
            }
            
            return null;
        }
    });

// Handle topic-based notifications
exports.sendTopicNotification = functions.database
    .ref('/topic_notifications/{notificationId}')
    .onCreate(async (snapshot, context) => {
        const notification = snapshot.val();
        const topic = notification.topic;
        
        if (!topic) {
            console.log('No topic provided');
            return null;
        }

        console.log('Processing notification for topic:', topic);
        console.log('Topic notification data:', notification);

        const message = {
            notification: {
                title: notification.title,
                body: notification.description,
                clickAction: 'FLUTTER_NOTIFICATION_CLICK'
            },
            data: {
                eventId: notification.eventId,
                clubId: notification.clubId,
                clubName: notification.clubName,
                type: notification.type,
                timestamp: notification.timestamp
            },
            topic: topic,
            android: {
                priority: 'high',
                notification: {
                    channelId: 'unifyu_notifications',
                    priority: 'high',
                    defaultSound: true,
                    defaultVibrateTimings: true
                }
            }
        };

        try {
            const response = await admin.messaging().send(message);
            console.log('Successfully sent topic message:', response);
            
            // Delete the notification after sending
            await snapshot.ref.remove();
            
            return response;
        } catch (error) {
            console.error('Error sending topic message:', error);
            // Log the full error details
            console.error('Error code:', error.code);
            console.error('Error message:', error.message);
            console.error('Error stack:', error.stack);
            
            return null;
        }
    });

// Send notifications when a new event is created
exports.notifyNewEvent = functions.database
    .ref('/events/{eventId}')
    .onCreate(async (snapshot, context) => {
        const eventId = context.params.eventId;
        const event = snapshot.val();

        if (!event) {
            console.log('No event data');
            return null;
        }

        console.log('New event created:', eventId);
        console.log('Event data:', event);

        try {
            // Get club details
            const clubSnapshot = await admin.database().ref(`/clubs/${event.clubId}`).once('value');
            const club = clubSnapshot.val();
            const clubName = club ? club.name : 'Club';

            // Notify all users via topic
            const message = {
                notification: {
                    title: `New Event: ${event.title}`,
                    body: `New event in ${clubName}: ${event.description}`
                },
                data: {
                    eventId: eventId,
                    clubId: event.clubId,
                    clubName: clubName,
                    type: 'new_event',
                    timestamp: Date.now().toString()
                },
                topic: 'all_users',
                android: {
                    priority: 'high',
                    notification: {
                        channelId: 'unifyu_notifications',
                        priority: 'high',
                        defaultSound: true,
                        defaultVibrateTimings: true
                    }
                }
            };

            const response = await admin.messaging().send(message);
            console.log('Successfully sent topic message:', response);
            
            // Also track notification history
            await admin.database().ref(`events/${eventId}/notifications`).push({
                title: message.notification.title,
                body: message.notification.body,
                sentToTopic: 'all_users',
                timestamp: Date.now()
            });
            
            return response;
        } catch (error) {
            console.error('Error sending event notification:', error);
            console.error('Error details:', error.message);
            return null;
        }
    }); 