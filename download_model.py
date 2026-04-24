"""
Download the Gemma model for Android app.
Run this script to download the model if not already present.
"""

import os
from huggingface_hub import hf_hub_download

def download_model():
    """Download the Gemma model from Hugging Face"""
    
    # Create models directory if it doesn't exist
    models_dir = "app/src/main/assets/models"
    os.makedirs(models_dir, exist_ok=True)
    
    model_filename = "Gemma3-1B-IT_multi-prefill-seq_q4_ekv2048.task"
    model_path = os.path.join(models_dir, model_filename)
    
    # Check if model already exists
    if os.path.exists(model_path):
        file_size = os.path.getsize(model_path) / (1024 * 1024)
        print(f"✅ Model already exists at: {model_path}")
        print(f"📊 File size: {file_size:.2f} MB")
        return
    
    print("📦 Downloading Gemma model (529 MB)...")
    print("⏳ This may take a few minutes depending on your internet speed")
    
    try:
        # Download from Hugging Face
        downloaded_path = hf_hub_download(
            repo_id="litert-community/Gemma3-1B-IT",
            filename=model_filename,
            local_dir=models_dir,
            local_dir_use_symlinks=False
        )
        
        file_size = os.path.getsize(downloaded_path) / (1024 * 1024)
        print(f"✅ Download complete!")
        print(f"📁 File saved to: {downloaded_path}")
        print(f"📊 File size: {file_size:.2f} MB")
        
    except Exception as e:
        print(f"❌ Download failed: {e}")
        print("\nAlternative: Download manually from:")
        print("https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/main/gemma-3-1b-it-q4_0-web.task")
        print("Save it to: app/src/main/assets/models/")

if __name__ == "__main__":
    download_model()