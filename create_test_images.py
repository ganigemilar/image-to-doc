from PIL import Image, ImageDraw, ImageFont
import os

os.makedirs("test_images", exist_ok=True)

# Create a few test images with different aspect ratios
for i, (w, h, color) in enumerate([
    (1920, 1080, "red"),      # Landscape
    (1080, 1920, "blue"),     # Portrait
    (1920, 1920, "green"),    # Square
]):
    img = Image.new("RGB", (w, h), color)
    draw = ImageDraw.Draw(img)
    # Add text
    try:
        font = ImageFont.truetype("arial.ttf", 60)
    except:
        font = ImageFont.load_default()
    draw.text((50, 50), f"Test Image {i+1}\n{w}x{h}", fill="white", font=font)
    img.save(f"test_images/test_{i+1}.png")
    print(f"Created test_images/test_{i+1}.png ({w}x{h})")

print("Test images created!")
