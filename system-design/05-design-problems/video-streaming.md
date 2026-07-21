The interviewer said: Design a video streaming platform. 200 million users. New season drops tonight. Everyone hits Play simultaneously. Your streaming service crashes at 8:00 PM. What happens?

 - Your single video server just received 200 million requests in 60 seconds. What happens to it?
 - User in Mumbai and user in New York both hit Play. Same video. Your server is in California. Both are buffering. Why?

The real problems at scale:

1. 200 million users hitting one server simultaneously
 - Central server receives 200 million requests in seconds. Bandwidth exhausted instantly. Every user sees buffering. Netflix trends on Twitter for wrong reasons.
 - Fix: Content Delivery Network CDN.
Netflix pre-loads popular content across thousands of CDN servers globally.
Mumbai user → served from Mumbai CDN server.
California server never touched.

2. Video file is 4GB. User's internet is slow. What happens?
 - Entire 4GB file cannot be streamed at once. Slow internet means constant buffering. User gives up. Cancels subscription.
 - Fix: Adaptive Bitrate Streaming.
Netflix pre-encodes every video in multiple quality levels.
4K. 1080p. 720p. 480p. 360p.
Your device constantly measures internet speed.
Speed drops → quality drops automatically.
Speed recovers → quality improves automatically.

3. Same popular video requested by millions simultaneously
 - Every request hitting origin server for same file. Massive redundant load.
 - Fix: Cache popular content at CDN edge servers in advance.
New season dropping tonight?
Netflix pre-warms CDN servers hours before release.
First request fetches from origin → cached at CDN.
Every subsequent request → served from cache in milliseconds.
Origin server sees almost zero load during peak.

4. Streaming service crashes mid-watch
 - User is 40 minutes into episode.
Service crashes. Comes back up.
User starts from beginning. Furious.
 - Fix: Store playback position in Redis per user per video.
Service crashes → recovers → reads last position from Redis.
User resumes exactly where they stopped.

The architecture:
 - User → API Gateway → Load Balancer → Streaming Service
→ CDN (video delivery) → Redis (playback position)
→ Adaptive Bitrate Player (client side)

- CDN handles 95% of all video traffic origin server barely touched
- Adaptive bitrate video quality matches user's connection automatically
- Redis playback position never lost
- Pre-warming CDN always ready before major releases
- Chaos Monkey Netflix intentionally breaks its own systems to test resilience