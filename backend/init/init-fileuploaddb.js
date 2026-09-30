console.log("Initializing replica set...");

rs.initiate({
    _id: "rs0",
    members: [
        {
            _id: 0,
            host: "file-upload-db:27017"
        }
    ]
});