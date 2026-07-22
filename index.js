const express = require('express');
const app = express();

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

let posts = [
    { id: 1, author: 'أيمن', content: 'مرحباً بكم في شبكة Facemook الجديدة!', likes: 5 }
];

app.get('/', (req, res) => {
    let postsHtml = posts.map(p => `
        <div style="background: white; padding: 15px; margin: 10px auto; max-width: 500px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: right;">
            <h3 style="color: #1877f2; margin: 0 0 5px 0;">${p.author}</h3>
            <p style="font-size: 16px; color: #333; margin: 0 0 10px 0;">${p.content}</p>
            <span style="color: #65676b; font-size: 14px;">❤️ ${p.likes} إعجاب</span>
        </div>
    `).join('');

    res.send(`
        <!DOCTYPE html>
        <html lang="ar" dir="rtl">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Facemook - شبكة التواصل الاجتماعي</title>
            <style>
                body { font-family: Arial, sans-serif; background-color: #f0f2f5; margin: 0; padding: 0; }
                header { background-color: #1877f2; color: white; padding: 15px; text-align: center; font-size: 24px; font-weight: bold; }
                .container { max-width: 600px; margin: 20px auto; padding: 10px; }
                .post-box { background: white; padding: 15px; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); margin-bottom: 20px; }
                textarea { width: 100%; height: 80px; padding: 10px; border: 1px solid #ccd0d5; border-radius: 6px; box-sizing: border-box; font-size: 15px; resize: none; }
                input { width: 100%; padding: 10px; margin-bottom: 10px; border: 1px solid #ccd0d5; border-radius: 6px; box-sizing: border-box; }
                button { background-color: #1877f2; color: white; border: none; padding: 10px 20px; border-radius: 6px; font-weight: bold; cursor: pointer; width: 100%; margin-top: 10px; }
                button:hover { background-color: #166fe5; }
            </style>
        </head>
        <body>
            <header>Facemook</header>
            <div class="container">
                <div class="post-box">
                    <h3>إنشاء منشور جديد</h3>
                    <form action="/add-post" method="POST">
                        <input type="text" name="author" placeholder="إسمك الكريم..." required>
                        <textarea name="content" placeholder="بماذا تفكر اليوم؟" required></textarea>
                        <button type="submit">نشر</button>
                    </form>
                </div>
                <h2 style="text-align: right; color: #4b4f56;">منشورات الأعضاء</h2>
                ${postsHtml}
            </div>
        </body>
        </html>
    `);
});

app.post('/add-post', (req, res) => {
    const { author, content } = req.body;
    if (author && content) {
        posts.unshift({ id: posts.length + 1, author, content, likes: 0 });
    }
    res.redirect('/');
});

const PORT = process.env.PORT || 3000;
app.listen(PORT, () => {
    console.log(`Facemook real network running on port ${PORT}`);
});
