-- Hierarchy catalog tables for class -> batch -> student navigation

CREATE TABLE IF NOT EXISTS classes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    class_number INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_classes_class_number UNIQUE (class_number)
);

CREATE TABLE IF NOT EXISTS batches (
    id BIGINT NOT NULL AUTO_INCREMENT,
    batch_number INT NOT NULL,
    class_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_batches_batch_number UNIQUE (batch_number),
    CONSTRAINT uq_batches_class_batch UNIQUE (class_id, batch_number),
    CONSTRAINT fk_batches_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE CASCADE
);

ALTER TABLE student
    ADD COLUMN IF NOT EXISTS class_id BIGINT NULL,
    ADD COLUMN IF NOT EXISTS batch_id BIGINT NULL;

ALTER TABLE student
    ADD CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES classes (id) ON DELETE SET NULL;

ALTER TABLE student
    ADD CONSTRAINT fk_student_batch FOREIGN KEY (batch_id) REFERENCES batches (id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_student_class_id ON student (class_id);
CREATE INDEX IF NOT EXISTS idx_student_batch_id ON student (batch_id);