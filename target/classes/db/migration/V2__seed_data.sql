-- ============================================================================
-- V2__seed_data.sql
-- Seed Data: 5 Restaurants, 10 Menu Items each (50 total), 20 Delivery Agents
-- ============================================================================

-- ----------------------------------------------------------------------------
-- 1. Insert 5 Restaurants
-- ----------------------------------------------------------------------------
INSERT INTO restaurants (id, name, cuisine_type, address, contact_number, latitude, longitude, operating_hours, is_active)
VALUES
(1, 'Spice Symphony Gourmet', 'North Indian & Mughlai', '124 Heritage Boulevard, Sector 14', '+91-9876543201', 28.6139, 77.2090, 
 '{"monday": {"open": "11:00", "close": "23:00"}, "tuesday": {"open": "11:00", "close": "23:00"}, "wednesday": {"open": "11:00", "close": "23:00"}, "thursday": {"open": "11:00", "close": "23:00"}, "friday": {"open": "11:00", "close": "23:59"}, "saturday": {"open": "11:00", "close": "23:59"}, "sunday": {"open": "11:00", "close": "23:59"}}'::jsonb, true),

(2, 'Bella Napoli Woodfire Pizzeria', 'Authentic Italian', '45 Corso Romano, Downtown Center', '+91-9876543202', 28.6250, 77.2150, 
 '{"monday": {"open": "12:00", "close": "23:00"}, "tuesday": {"open": "12:00", "close": "23:00"}, "wednesday": {"open": "12:00", "close": "23:00"}, "thursday": {"open": "12:00", "close": "23:00"}, "friday": {"open": "12:00", "close": "23:30"}, "saturday": {"open": "12:00", "close": "23:30"}, "sunday": {"open": "12:00", "close": "23:00"}}'::jsonb, true),

(3, 'Dragon Wok Pan-Asian', 'Chinese & Thai Street Food', '88 Silk Route Plaza, Tech Park', '+91-9876543203', 28.6320, 77.2200, 
 '{"monday": {"open": "11:30", "close": "22:30"}, "tuesday": {"open": "11:30", "close": "22:30"}, "wednesday": {"open": "11:30", "close": "22:30"}, "thursday": {"open": "11:30", "close": "22:30"}, "friday": {"open": "11:30", "close": "23:00"}, "saturday": {"open": "11:30", "close": "23:00"}, "sunday": {"open": "11:30", "close": "22:30"}}'::jsonb, true),

(4, 'Green Delight Organic & Salads', 'Healthy Bowls & Vegan', '19 Eco Garden Lane, Green Avenue', '+91-9876543204', 28.6080, 77.2020, 
 '{"monday": {"open": "08:00", "close": "21:00"}, "tuesday": {"open": "08:00", "close": "21:00"}, "wednesday": {"open": "08:00", "close": "21:00"}, "thursday": {"open": "08:00", "close": "21:00"}, "friday": {"open": "08:00", "close": "21:00"}, "saturday": {"open": "08:00", "close": "22:00"}, "sunday": {"open": "08:00", "close": "22:00"}}'::jsonb, true),

(5, 'Burger Crave Smokehouse', 'Gourmet Burgers & BBQ', '73 Grill Street, West End Quarter', '+91-9876543205', 28.6400, 77.2250, 
 '{"monday": {"open": "11:00", "close": "23:00"}, "tuesday": {"open": "11:00", "close": "23:00"}, "wednesday": {"open": "11:00", "close": "23:00"}, "thursday": {"open": "11:00", "close": "23:00"}, "friday": {"open": "11:00", "close": "23:59"}, "saturday": {"open": "11:00", "close": "23:59"}, "sunday": {"open": "11:00", "close": "23:00"}}'::jsonb, true)
ON CONFLICT (id) DO NOTHING;

SELECT setval('restaurants_id_seq', (SELECT MAX(id) FROM restaurants));

-- ----------------------------------------------------------------------------
-- 2. Insert 10 Menu Items for Each of the 5 Restaurants (50 items total)
-- ----------------------------------------------------------------------------
-- Restaurant 1: Spice Symphony Gourmet (Items 1 - 10)
INSERT INTO menu_items (id, restaurant_id, name, description, price, prep_time_minutes, is_available) VALUES
(1, 1, 'Butter Chicken Royale', 'Tender tandoori chicken simmered in rich creamy tomato and butter gravy', 14.99, 18, true),
(2, 1, 'Paneer Tikka Masala', 'Charcoal-grilled cottage cheese cubes in spiced aromatic gravy', 12.99, 15, true),
(3, 1, 'Dum Hyderabadi Biryani', 'Fragrant basmati rice layered with spiced meat and saffron', 15.50, 20, true),
(4, 1, 'Dal Makhani Gold', 'Slow-cooked black lentils with cream and organic churned butter', 9.99, 12, true),
(5, 1, 'Garlic Butter Naan', 'Freshly baked tandoori flatbread brushed with roasted garlic butter', 3.50, 6, true),
(6, 1, 'Tandoori Sizzler Platter', 'Assortment of seekh kebabs, chicken tikka, and grilled veggies', 18.00, 22, true),
(7, 1, 'Rogan Josh Kashmiri', 'Slow-braised tender lamb cooked with Kashmiri red chilies', 16.99, 20, true),
(8, 1, 'Palak Paneer Velvet', 'Fresh spinach puree infused with mild spices and paneer cubes', 11.99, 14, true),
(9, 1, 'Vegetable Pulao Feast', 'Mildly spiced aromatic rice with seasonal garden vegetables', 8.50, 10, true),
(10, 1, 'Gulab Jamun with Rabri', 'Warm caramelized milk dumplings served with reduced saffron milk', 5.99, 5, true),

-- Restaurant 2: Bella Napoli Woodfire Pizzeria (Items 11 - 20)
(11, 2, 'Margherita Verace', 'San Marzano tomatoes, fresh mozzarella fior di latte, basil, EVOO', 13.50, 12, true),
(12, 2, 'Diavola Pepperoni Crunch', 'Spicy Calabrian salami, mozzarella, chili flakes, tomato sauce', 15.99, 14, true),
(13, 2, 'Quattro Formaggi', 'Mozzarella, gorgonzola, parmesan reggiano, and fontina cheese', 16.50, 13, true),
(14, 2, 'Truffle Funghi Tartufo', 'Wild forest mushrooms, white truffle oil, roasted garlic cream', 17.00, 15, true),
(15, 2, 'Fettuccine Creamy Alfredo', 'Handmade egg pasta tossed with parmesan cream and parsley', 14.00, 12, true),
(16, 2, 'Penne Alla Vodka', 'Penne pasta in silky tomato-cream sauce with fresh herbs', 13.50, 12, true),
(17, 2, 'Crispy Arancini Balls', 'Saffron risotto balls stuffed with smoked mozzarella', 8.99, 10, true),
(18, 2, 'Classic Caesar Salad', 'Romaine lettuce, garlic croutons, shaved parmesan, house dressing', 7.99, 8, true),
(19, 2, 'Garlic Breadstick Swirls', 'Oven-baked dough sticks brushed with garlic butter and herbs', 4.99, 6, true),
(20, 2, 'Tiramisu Tradizionale', 'Espresso-soaked savoiardi layered with mascarpone cream', 6.50, 5, true),

-- Restaurant 3: Dragon Wok Pan-Asian (Items 21 - 30)
(21, 3, 'Kung Pao Chicken Wok', 'Diced chicken, roasted peanuts, chili peppers, scallions', 13.99, 14, true),
(22, 3, 'Hakka Chilli Garlic Noodles', 'Wok-tossed egg noodles with crisp vegetables and fiery garlic oil', 10.99, 10, true),
(23, 3, 'Pad Thai Supreme', 'Rice noodles, jumbo prawns, bean sprouts, tamarind crushed peanuts', 14.50, 15, true),
(24, 3, 'Crispy Honey Chilli Lotus Stem', 'Crispy lotus roots glazed in sweet chili honey sauce', 9.50, 10, true),
(25, 3, 'Steamed Dim Sum Basket', 'Assorted vegetable and chicken dumplings with spicy sesame dip', 11.00, 12, true),
(26, 3, 'Manchurian Gravy Bowl', 'Golden fried veggie balls in spicy dark soy-garlic gravy', 11.50, 12, true),
(27, 3, 'Thai Green Curry Jasmine Rice', 'Coconut milk curry with bamboo shoots, basil, steamed jasmine rice', 14.99, 16, true),
(28, 3, 'Crispy Spring Rolls', 'Golden rolls stuffed with glass noodles and shredded cabbage', 6.99, 8, true),
(29, 3, 'Egg Fried Rice Deluxe', 'Fragrant rice stir-fried with scallions, eggs, and light soy', 9.99, 10, true),
(30, 3, 'Darsaan Honey Noodles', 'Crispy fried flat noodles with vanilla ice cream and sesame seeds', 5.50, 5, true),

-- Restaurant 4: Green Delight Organic & Salads (Items 31 - 40)
(31, 4, 'Avocado Superfood Bowl', 'Quinoa, Hass avocado, cherry tomatoes, baby spinach, tahini dressing', 12.50, 10, true),
(32, 4, 'Mediterranean Falafel Wrap', 'Crispy baked chickpea falafel, hummus, cucumber, whole wheat pita', 10.99, 10, true),
(33, 4, 'Kale & Roasted Walnut Salad', 'Tuscan kale, candied walnuts, dried cranberries, balsamic glaze', 11.00, 8, true),
(34, 4, 'Roasted Butternut Squash Soup', 'Creamy organic butternut soup seasoned with nutmeg and thyme', 7.50, 8, true),
(35, 4, 'Chia Seed Acai Smoothie Bowl', 'Organic acai berry blend topped with chia seeds, granola, banana', 9.99, 6, true),
(36, 4, 'Grilled Tofu Teriyaki Bowl', 'Organic firm tofu, brown rice, broccoli, edamame, ginger sauce', 13.00, 12, true),
(37, 4, 'Sweet Potato Wedges Herb Dip', 'Air-fried sweet potato wedges served with vegan garlic aioli', 5.99, 10, true),
(38, 4, 'Zucchini Pesto Zoodles', 'Spiralized zucchini tossed with house-made pine nut basil pesto', 11.50, 10, true),
(39, 4, 'Cold Pressed Detox Green Juice', 'Cucumber, green apple, celery, spinach, ginger, lemon', 5.50, 4, true),
(40, 4, 'Raw Vegan Chocolate Tart', 'Almond flour crust filled with rich raw cocoa cream', 6.50, 5, true),

-- Restaurant 5: Burger Crave Smokehouse (Items 41 - 50)
(41, 5, 'The Double Smash Cheddar Burger', 'Two smashed beef patties, melted aged cheddar, secret crave sauce', 14.99, 14, true),
(42, 5, 'Crispy Nashville Hot Chicken Burger', 'Crispy battered breast, spicy chili oil, coleslaw, dill pickles', 13.99, 13, true),
(43, 5, 'Smoked BBQ Bacon Beast', 'Smoked patty, crispy maple bacon, onion rings, hickory BBQ sauce', 15.50, 15, true),
(44, 5, 'Portobello Mushroom Swiss Melt', 'Grilled portobello mushroom cap, melted swiss cheese, truffle mayo', 12.99, 12, true),
(45, 5, 'Loaded Pulled Pork Fries', 'Seasoned skin-on fries topped with 12-hour smoked pork and cheese sauce', 9.50, 10, true),
(46, 5, 'Cajun Crispy Onion Rings', 'Beer-battered jumbo onion rings with cajun ranch dip', 6.00, 8, true),
(47, 5, 'Smoked Chicken Wings (8 pcs)', 'Hickory-smoked jumbo wings tossed in buffalo sauce', 11.99, 14, true),
(48, 5, 'Truffle Parmesan Fries', 'Crispy fries tossed in white truffle oil and grated grana padano', 6.99, 7, true),
(49, 5, 'Craft Root Beer Float', 'Sparkling artisanal root beer with creamy vanilla bean ice cream', 4.99, 4, true),
(50, 5, 'Molten Chocolate Fudge Shake', 'Rich thick milkshake with chocolate fudge and brownie chunks', 6.50, 5, true)
ON CONFLICT (id) DO NOTHING;

SELECT setval('menu_items_id_seq', (SELECT MAX(id) FROM menu_items));

-- ----------------------------------------------------------------------------
-- 3. Insert 20 Delivery Pickup Agents
-- Distributed around restaurant coordinates for distance simulation (2, 3, 4, 5 km)
-- ----------------------------------------------------------------------------
INSERT INTO delivery_agents (id, name, phone, vehicle_type, status, current_latitude, current_longitude, rating)
VALUES
(1, 'Aarav Sharma', '+91-9800000001', 'Electric Scooter', 'AVAILABLE', 28.6150, 77.2100, 4.90),
(2, 'Rohit Verma', '+91-9800000002', 'Honda Activa', 'AVAILABLE', 28.6200, 77.2120, 4.85),
(3, 'Vikram Singh', '+91-9800000003', 'Hero Electric', 'AVAILABLE', 28.6270, 77.2170, 4.88),
(4, 'Deepak Patel', '+91-9800000004', 'TVS Jupiter', 'AVAILABLE', 28.6300, 77.2190, 4.75),
(5, 'Suresh Kumar', '+91-9800000005', 'Yamaha FZ', 'AVAILABLE', 28.6340, 77.2210, 4.92),
(6, 'Amit Yadav', '+91-9800000006', 'Ather 450X', 'AVAILABLE', 28.6100, 77.2040, 4.80),
(7, 'Manoj Tiwari', '+91-9800000007', 'Bajaj Pulsar', 'AVAILABLE', 28.6380, 77.2230, 4.78),
(8, 'Karan Malhotra', '+91-9800000008', 'Electric Scooter', 'AVAILABLE', 28.6160, 77.2110, 4.95),
(9, 'Rajesh Joshi', '+91-9800000009', 'Honda Shine', 'AVAILABLE', 28.6220, 77.2140, 4.82),
(10, 'Sunil Gupta', '+91-9800000010', 'Ola S1 Pro', 'AVAILABLE', 28.6280, 77.2180, 4.89),
(11, 'Anil Chauhan', '+91-9800000011', 'TVS Apache', 'AVAILABLE', 28.6310, 77.2205, 4.76),
(12, 'Pooja Rani', '+91-9800000012', 'Electric Scooter', 'AVAILABLE', 28.6090, 77.2030, 4.93),
(13, 'Neha Kumari', '+91-9800000013', 'Honda Activa 6G', 'AVAILABLE', 28.6390, 77.2240, 4.87),
(14, 'Gaurav Mishra', '+91-9800000014', 'Hero Splendor', 'AVAILABLE', 28.6170, 77.2115, 4.81),
(15, 'Vishal Saxena', '+91-9800000015', 'Ather 450S', 'AVAILABLE', 28.6240, 77.2155, 4.84),
(16, 'Sachin Pandey', '+91-9800000016', 'Bajaj Chetak EV', 'AVAILABLE', 28.6290, 77.2185, 4.91),
(17, 'Mohit Nair', '+91-9800000017', 'Yamaha RayZR', 'AVAILABLE', 28.6330, 77.2215, 4.79),
(18, 'Praveen Das', '+91-9800000018', 'TVS iQube', 'AVAILABLE', 28.6110, 77.2050, 4.86),
(19, 'Sanjay Rawat', '+91-9800000019', 'Hero Passion', 'AVAILABLE', 28.6410, 77.2260, 4.77),
(20, 'Harish Reddy', '+91-9800000020', 'Electric Scooter', 'AVAILABLE', 28.6180, 77.2125, 4.94)
ON CONFLICT (id) DO NOTHING;

SELECT setval('delivery_agents_id_seq', (SELECT MAX(id) FROM delivery_agents));
