# Big Data Project: Google Local Reviews 🇺🇸 🗺️ 📈

This repository hosts the code for the Big Data Project exam @ UniBo on [Google Local Reviews](https://mcauleylab.ucsd.edu/public_datasets/gdrive/googlelocal/#sample-review).

This Dataset contains review information on Google map (ratings, text, images, etc.), business metadata (address, geographical info, descriptions, category information, price, open hours, and MISC info), and links (relative businesses) up to Sep 2021 in the United States.

Our analysis is limited to the following states: Alabama, Mississippi, New Hampshire, New Mexico and Washington.
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
- **[➰ FK] `gmap_id`: ID of the business**;

### Metadata

- `name`: name of the business;
- `address`: address of the business;
- **[➰ PK] `gmap_id`: ID of the business**;
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

## Devos

### How to get the dataset?

UniBo member? You can download full dataset from [here](https://liveunibo-my.sharepoint.com/:f:/g/personal/luca_tassinari10_studio_unibo_it/ErdSkAIdiHlAqnXVcEfHHMYBJxc80u6gVmfz6fmBMwCN_A?e=0cXkhT).

Otherwise:

- Download from [here](https://mcauleylab.ucsd.edu/public_datasets/gdrive/googlelocal/#complete-data) all the `ndjson` files;
  - only Alabama, Mississippi, New Hampshire, New Mexico and Washington are used in this project;
- Merge together reviews files with `cat`, like `cat reviews-*.ndjson > reviews.ndjson`;
- Get rid of reviews < 2015 (`1420070400000` unix epoch time corresponds to `2015-01-01 00:00:00 UTC`):

  ```bash
  pv -l reviews.ndjson | jq -c 'select(.time != null and .time > 1420070400000)' > reviews.ndjson
  ```

### Dataset Sample

Dataset sample is obtained using the following command:

```bash
pv -l reviews.ndjson | awk 'BEGIN{srand(42)} rand()<=0.01 {print}' > sample.ndjson
```

### Jupyter Notebooks

- Python 3.11 version
- install from `requirements.txt`
- make sure to install: `python -m spylon_kernel install --user`
- to open a jupyter notebook: `jupyter notebook`

### Cluster operations

To create a new AWS profile create a `.env` file in the project root with the following content:

```plaintext
ACCESS_KEY_ID=<access_key_id>
SECRET_ACCESS_KEY=<secret_access_key>
SESSION_TOKEN=<session_token>
```

and run

```bash
./gradlew createProfile
```

Or, **alternatively**:

```bash
./gradlew createProfile -PaccessKeyId=<access_key_id> -PsecretAccessKey=<secret_access_key> -PsessionToken=<session_token>
```

This command will create a new profile named with the same project name.

#### Dataset load

```bash
# from the project root
aws s3api list-buckets --profile <profile-name> # list buckets
aws s3api create-bucket --bucket google-local-reviews-analysis --profile <profile-name> # create new bucket
# copy metadata ndjson file
aws s3 cp ./dataset/metadata.ndjson s3://google-local-reviews-analysis/dataset/metadata.ndjson --profile <profile-name>
# load reviews ndjson file
aws s3 cp ./dataset/reviews.ndjson s3://google-local-reviews-analysis/dataset/reviews.ndjson --profile <profile-name>
```

### Useful links

- [Spark documentation](https://spark.apache.org/docs/latest/api/scala/index.html)
- [RDD programming guide](https://spark.apache.org/docs/latest/rdd-programming-guide.html)

## References

- [_UCTopic: Unsupervised Contrastive Learning for Phrase Representations and Topic Mining_](https://aclanthology.org/2022.acl-long.426.pdf).

  Jiacheng Li, Jingbo Shang, Julian McAuley

  Annual Meeting of the Association for Computational Linguistics (ACL), 2022

- [_Personalized Showcases: Generating Multi-Modal Explanations for Recommendations_](https://arxiv.org/pdf/2207.00422)

  An Yan, Zhankui He, Jiacheng Li, Tianyang Zhang, Julian Mcauley

  The 46th International ACM SIGIR Conference on Research and Development in Information Retrieval (SIGIR), 2023
