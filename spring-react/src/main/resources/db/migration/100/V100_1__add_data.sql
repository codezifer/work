INSERT INTO tutorials.tutorial
(id, title, description, published)
VALUES
(nextval('tutorials.tutorial_id_seq'::regclass), 'React', 'React example tutorial', false),
(nextval('tutorials.tutorial_id_seq'::regclass), 'Angular', 'Angular example tutorial', false),
(nextval('tutorials.tutorial_id_seq'::regclass), 'Vue', 'Vue example tutorial', false)
;