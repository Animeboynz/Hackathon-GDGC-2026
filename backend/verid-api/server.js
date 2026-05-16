const express = require("express");
const sqlite3 = require("sqlite3").verbose();
const cors = require("cors");
const crypto = require("crypto");

const app = express();
app.use(cors());
app.use(express.json());

// Your SQLite database path
const db = new sqlite3.Database("./hackthon.db");

// Test route
app.get("/", (req, res) => {
  res.json({
    message: "VerID API is running"
  });
});

// Register/store a new user and create a digital ID
app.post("/register", (req, res) => {
  const {
    full_name,
    date_of_birth,
    nationality,
    passport_num,
    ethnicity,
    photo_data
  } = req.body;

  if (!full_name || !date_of_birth || !nationality || !passport_num) {
    return res.status(400).json({
      success: false,
      message: "Missing required user information"
    });
  }

  const cryptKey = crypto.randomBytes(16).toString("hex");
  const publicId = "verid-" + crypto.randomBytes(8).toString("hex");
  const expiresDate = "2027-01-01";

  db.run(
    `
    INSERT INTO Users (
      full_name,
      Date_of_birth,
      nationality,
      Passport_num,
      Ethnicity,
      Crypt_key,
      Photo_data
    )
    VALUES (?, ?, ?, ?, ?, ?, ?)
    `,
    [
      full_name,
      date_of_birth,
      nationality,
      passport_num,
      ethnicity || "Not specified",
      cryptKey,
      photo_data || "demo-photo-data"
    ],
    function (err) {
      if (err) {
        return res.status(500).json({
          success: false,
          message: "Error saving user",
          error: err.message
        });
      }

      const userId = this.lastID;

      db.run(
        `
        INSERT INTO digital_id (
          user_id,
          expires_date,
          public_id,
          status
        )
        VALUES (?, ?, ?, ?)
        `,
        [userId, expiresDate, publicId, "active"],
        function (err) {
          if (err) {
            return res.status(500).json({
              success: false,
              message: "User saved, but error creating digital ID",
              error: err.message
            });
          }

          return res.json({
            success: true,
            message: "User registered and digital ID created",
            user_id: userId,
            public_id: publicId,
            status: "active",
            expires_date: expiresDate
          });
        }
      );
    }
  );
});

// Company verifies a digital ID
app.get("/verify/:publicId", (req, res) => {
  const publicId = req.params.publicId;
  const apiKey = req.headers["x-api-key"];

  db.get(
    `
    SELECT *
    FROM api_clients
    WHERE api_key = ? AND status = 'active'
    `,
    [apiKey],
    (err, company) => {
      if (err) {
        return res.status(500).json({
          valid: false,
          message: "Database error checking API key",
          error: err.message
        });
      }

      if (!company) {
        return res.status(401).json({
          valid: false,
          message: "Invalid or missing API key"
        });
      }

      db.get(
        `
        SELECT
          digital_id.VerID,
          digital_id.public_id,
          digital_id.status,
          digital_id.expires_date,
          Users.full_name,
          Users.Date_of_birth,
          Users.nationality
        FROM digital_id
        JOIN Users ON digital_id.user_id = Users.user_id
        WHERE digital_id.public_id = ?
        `,
        [publicId],
        (err, record) => {
          if (err) {
            return res.status(500).json({
              valid: false,
              message: "Database error checking digital ID",
              error: err.message
            });
          }

          if (!record) {
            return res.json({
              valid: false,
              message: "Digital ID not found"
            });
          }

          if (record.status !== "active") {
            return res.json({
              valid: false,
              message: "Digital ID is not active"
            });
          }

          return res.json({
            valid: true,
            message: "Digital ID is valid",
            verified_by: "VerID",
            public_id: record.public_id,
            full_name: record.full_name,
            date_of_birth: record.Date_of_birth,
            nationality: record.nationality,
            expires_date: record.expires_date
          });
        }
      );
    }
  );
});

// View all users for testing
app.get("/users", (req, res) => {
  db.all("SELECT * FROM Users", [], (err, rows) => {
    if (err) {
      return res.status(500).json({
        message: "Error loading users",
        error: err.message
      });
    }

    res.json(rows);
  });
});

app.listen(3000, () => {
  console.log("VerID API running at http://localhost:3000");
});
