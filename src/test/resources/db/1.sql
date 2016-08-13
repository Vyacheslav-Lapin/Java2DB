CREATE TABLE Contact (
  id         INT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(60) NOT NULL,
  last_name  VARCHAR(40) NOT NULL,
  birth_date DATE,

  UNIQUE (first_name, last_name)
);