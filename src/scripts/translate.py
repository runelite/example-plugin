import argparse
from mtranslate import translate

def translate_text(text, source_lang, target_lang):
    translated_text = translate(text, source_lang, target_lang)
    return translated_text

if __name__ == '__main__':
    # Define command-line arguments
    parser = argparse.ArgumentParser(description='Translate text using mtranslate')
    parser.add_argument('text', type=str, help='Text to be translated')
    parser.add_argument('source_lang', type=str, help='Source language code')
    parser.add_argument('target_lang', type=str, help='Target language code')
    args = parser.parse_args()

    # Call the translation function
    translated_text = translate_text(args.text, args.source_lang, args.target_lang)

    # Print the translation
    print(f'Translated text: {translated_text}')
