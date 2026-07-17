from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parents[1]
SCREENS = ROOT / "screens"
OUTPUT = SCREENS / "15-ui-overview.png"

PAGES = [
    ("01 登录", "01-login-default.png"),
    ("02 绑定账号", "02-bind-account-default.png"),
    ("03 实时看板", "03-dashboard-default.png"),
    ("04 生产分析", "04-production-analysis-default.png"),
    ("05 产品追溯", "05-product-trace-default.png"),
    ("06 个人中心", "06-profile-default.png"),
]

COLS = 3
ROWS = 2
CARD_WIDTH = 360
IMAGE_HEIGHT = 820
LABEL_HEIGHT = 52
GAP = 24
MARGIN = 32
HEADER_HEIGHT = 92


def load_font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    candidates = [
        Path(r"C:\Windows\Fonts\msyhbd.ttc" if bold else r"C:\Windows\Fonts\msyh.ttc"),
        Path(r"C:\Windows\Fonts\simhei.ttf"),
    ]
    for candidate in candidates:
        if candidate.exists():
            return ImageFont.truetype(str(candidate), size)
    return ImageFont.load_default()


def main() -> None:
    width = MARGIN * 2 + COLS * CARD_WIDTH + (COLS - 1) * GAP
    height = HEADER_HEIGHT + MARGIN + ROWS * (LABEL_HEIGHT + IMAGE_HEIGHT) + (ROWS - 1) * GAP + MARGIN
    canvas = Image.new("RGB", (width, height), "#F8FAFC")
    draw = ImageDraw.Draw(canvas)
    title_font = load_font(32, bold=True)
    label_font = load_font(20, bold=True)
    meta_font = load_font(15)

    draw.text((MARGIN, 24), "羽毛球 MES 微信小程序 · Stitch UI 总览", fill="#115E59", font=title_font)
    draw.text((MARGIN, 64), "六张默认页面机械拼接；原始页面未经过 AI 重绘或内容修改", fill="#64748B", font=meta_font)

    for index, (label, filename) in enumerate(PAGES):
        row, col = divmod(index, COLS)
        x = MARGIN + col * (CARD_WIDTH + GAP)
        y = HEADER_HEIGHT + MARGIN + row * (LABEL_HEIGHT + IMAGE_HEIGHT + GAP)

        draw.rounded_rectangle(
            (x, y, x + CARD_WIDTH, y + LABEL_HEIGHT + IMAGE_HEIGHT),
            radius=14,
            fill="#FFFFFF",
            outline="#E2E8F0",
            width=2,
        )
        draw.text((x + 16, y + 13), label, fill="#172033", font=label_font)

        with Image.open(SCREENS / filename) as source:
            rgb = source.convert("RGB")
            rgb.thumbnail((CARD_WIDTH - 4, IMAGE_HEIGHT - 4), Image.Resampling.LANCZOS)
            image_x = x + (CARD_WIDTH - rgb.width) // 2
            image_y = y + LABEL_HEIGHT + (IMAGE_HEIGHT - rgb.height) // 2
            canvas.paste(rgb, (image_x, image_y))

    canvas.save(OUTPUT, format="PNG", optimize=True)


if __name__ == "__main__":
    main()
