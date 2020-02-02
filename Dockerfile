FROM scratch
COPY /data /data
COPY target/app /app
CMD ["/app"]