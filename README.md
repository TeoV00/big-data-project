# Big Data Project: Google Local Reviews

This repository hosts the code for the Big Data Project on [Google Local Reviews](https://mcauleylab.ucsd.edu/public_datasets/gdrive/googlelocal/#sample-review).

This Dataset contains review information on Google map (ratings, text, images, etc.), business metadata (address, geographical info, descriptions, category information, price, open hours, and MISC info), and links (relative businesses) up to Sep 2021 in the United States.

Our analysis is limited to the most three important states: California, New York, and Texas.
We focus on the reviews and metadata of businesses in these states.

## Dataset

### Reviews

- `user_id`: ID of the reviewer;
- `name`: name of the reviwer;
- `time`: time of the review (unix time);
- `rating`: rating of the business;
- `text`: text of the review;
- `pics`: pictures of the review;
- `resp`: business response to the review including unix time and text of the response;
  - `time`: time of the response (unix time);
  - `text`: text of the response;
- ➰ **`gmap_id`: ID of the business**;

### Metadata

- `name`: name of the business;
- `address`: address of the business;
- ➰ **`gmap_id`: ID of the business**;
- `description`: description of the business;
- `latitude`: latitude of the business;
- `longitude`: longitude of the business;
- `category`: categories of the business;
- `avg_rating`: average rating of the business;
- `num_of_reviews`: number of reviews;
- `price`: price of the business;
- `hours`: open hours;
- `MISC`: MISC information;
- `state`: the current status of the business (e.g., `permanently closed`);
- `relative_results`: relative businesses recommended by Google;
- `url`: URL of the business;

## Jobs

### Job 1

The goal of this job is to understand, year by year, whether greater frequency in responding to reviews has an impact on the average rating received.

Specifically:

- For each year and business, the average reviews, rate, and average response time are calculated;
- Based on the rate and average response time, an additional attribute "response strategy" is calculated that categorizes the business in a particular year into four categories ("Rapid and frequent," "Slow but frequent," "Occasional," or "Rare or none");
- Aggregation based on the "response strategy," year, and state to get the average rate and number of businesses within the category.

### Job 2

Job 2 computes, annually, for each business state, category and price range the average rating, assigning a rating.

In detail:

- for each business, the average rating of the reviews is calculated by grouping them by year;
- aggregating by business category, state and price range the average rating of the reviews is calculated;
- based on the average rating an additional attribute "business suggestion" is processed, which provides a rating on the business categories, as follows:
  - average rating < 2: "Not recommended"
  - average rating 2-3.5: "Discreet"
  - average rating 3.5-4.5: "Recommended"
  - average rating > 4.5: "Highly recommended"

## Other information

### Dataset Sample

Dataset sample is obtained using the following command:

```bash
pv -s 50G reviews.ndjson | awk 'BEGIN{srand(42)} rand()<=0.01 {print}' > sample.ndjson
```

### Jupyter Notebooks

- Python 3.11 version
- install from requirements.txt
- make sure to install: `python -m spylon_kernel install --user`
- to open a jupyter notebook: `jupyter notebook`
