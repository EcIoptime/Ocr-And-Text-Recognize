import spacy
import json
import os

# Load the spaCy model
# model_path = "model"


# def set(model_path):
#     os.environ['model'] = model_path
#     nlp = spacy.load(model_path)

# nlp = spacy.load(os.environ['model'])

# Define a function to process text
def process_text(text ,modelPath):
    nlp = spacy.load(modelPath)
    doc = nlp(text)
    # Perform any custom logic with the spaCy model
    # Create a dictionary to store the extracted entities
    entities = {}

    # Populate the dictionary with extracted entities
    for ent in doc.ents:
        entities[ent.label_] = ent.text

    # Convert the dictionary to a JSON object
    json_response = json.dumps(entities)

    return json_response


def detect_text_classify(text, modelPath):
    nlp = spacy.load(modelPath)
    doc = nlp(text)

    # Get the predicted probabilities for each category
    predicted_cats = doc.cats

    # Convert the predicted_cats dictionary to a JSON object
    json_response = json.dumps(predicted_cats)

    return json_response
