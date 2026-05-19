FROM node:22-alpine AS build

WORKDIR /app

COPY package*.json ./
RUN npm ci

ARG VITE_NOTES_SERVICE_URL=http://localhost:8081
ARG VITE_DEADLINES_SERVICE_URL=http://localhost:8082
ENV VITE_NOTES_SERVICE_URL=$VITE_NOTES_SERVICE_URL
ENV VITE_DEADLINES_SERVICE_URL=$VITE_DEADLINES_SERVICE_URL

COPY . .
RUN npm run build

FROM nginx:1.27-alpine

COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html

EXPOSE 80
