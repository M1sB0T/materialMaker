-- 模板种子数据（INSERT IGNORE 保证重复执行幂等）
INSERT IGNORE INTO templates
    (id, title, category, cover_url, description, required_image_count,
     accepted_mime_types, max_file_size_bytes, result_asset)
VALUES
    ('demo-postcard', '旅行明信片', '创意图片', '/demo/covers/demo-postcard.png',
     '上传 2 张旅行照片，模拟生成一张复古明信片', 2, 'image/jpeg,image/png', 5242880, 'demo-postcard.png'),
    ('demo-avatar', '卡通头像', '创意图片', '/demo/covers/demo-avatar.png',
     '上传 1 张人像照片，模拟生成一张卡通风格头像', 1, 'image/jpeg,image/png', 5242880, 'demo-avatar.png'),
    ('demo-wallpaper', '手机壁纸', '创意图片', '/demo/covers/demo-wallpaper.png',
     '上传 1 张照片，模拟生成一张手机竖屏壁纸', 1, 'image/jpeg,image/png', 5242880, 'demo-wallpaper.png'),
    ('demo-meme', '表情包', '创意图片', '/demo/covers/demo-meme.png',
     '上传 1 张图片，模拟生成一张斗图表情包', 1, 'image/jpeg,image/png', 5242880, 'demo-meme.png'),
    ('demo-photo-frame', '全家福相框', '家庭记忆', '/demo/covers/demo-photo-frame.png',
     '上传 3 张家庭照片，模拟生成一张带相框的全家福', 3, 'image/jpeg,image/png', 5242880, 'demo-photo-frame.png'),
    ('demo-collage', '旅行拼图', '家庭记忆', '/demo/covers/demo-collage.png',
     '上传 4 张照片，模拟生成一张四宫格旅行拼图', 4, 'image/jpeg,image/png', 5242880, 'demo-collage.png'),
    ('demo-banner', '商品主图', '电商素材', '/demo/covers/demo-banner.png',
     '上传 1 张商品照片，模拟生成一张电商主图', 1, 'image/jpeg,image/png', 5242880, 'demo-banner.png');
