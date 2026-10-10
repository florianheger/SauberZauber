# General Description
The app should track different tasks in the household, like washing clothes, empty the trash and so on. The goal is to see, who did which tasks how often, in order to share the housework fair. It is not about scheduling. Its only purpose is to note, who did which tasks when and how often.

The app is used for one household with two peoples. There is no need for functionbality like multiple households or more than two users.

The UI is in german.

# Tech Stack
The app is intended to be runned completely in docker using docker-compose. 

## Backend
The Backend should be written in Java Spring. Spring security should be used for the authentification. 

## Frontend
The Frontend should be written in Angular. It is a mobile-first web-app. The UI sould be responsible fort both desktop and smartphone. It should be installable as a PWA on both Android and IOS.