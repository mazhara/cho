CREATE TYPE author_type AS ENUM ('Author', 'Composer', 'Editor', 'Illustrator');


-- Create the authors table
CREATE TABLE authors (
    author_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    first_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    author_type author_type NOT NULL
);

-- Insert sample data for existing authors
INSERT INTO authors (author_id, first_name, last_name, author_type)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'Jane', 'Doe', 'Author');
