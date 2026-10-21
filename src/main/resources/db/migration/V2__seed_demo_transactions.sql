-- Made-up transactions for the demo and tests. Dates are relative, so they stay inside the dispute window.
insert into transactions (reference, amount, currency, merchant, booked_on) values
    ('TX-1001', 89.95,  'EUR', 'Fietsen Online BV',     current_date - 12),
    ('TX-1002', 12.99,  'EUR', 'StreamFlix',            current_date - 5),
    ('TX-1003', 249.00, 'EUR', 'Elektro Megastore',     current_date - 30),
    ('TX-1004', 45.50,  'EUR', 'Pizzeria Da Marco',     current_date - 2),
    ('TX-1005', 1299.00,'EUR', 'Laptop Direct',         current_date - 60),
    ('TX-1006', 19.99,  'EUR', 'Cloud Storage Plus',    current_date - 40),
    ('TX-0999', 75.00,  'EUR', 'Old Bookshop',          current_date - 200);
