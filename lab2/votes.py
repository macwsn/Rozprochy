from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="Doodle Voting API")

polls = {}
votes = {}

class PollCreate(BaseModel):
    title: str
    options: list[str]

class Vote(BaseModel):
    voter_name: str
    option: str

@app.post("/polls")
def create_poll(poll: PollCreate):
    poll_id = f"poll_{len(polls) + 1}"
    polls[poll_id] = {"title": poll.title, "options": poll.options}
    votes[poll_id] = {}
    return {"id": poll_id, **polls[poll_id]}

@app.get("/polls")
def get_polls():
    return [{"id": pid, **data} for pid, data in polls.items()]

@app.get("/polls/{poll_id}")
def get_poll(poll_id: str):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Poll not found")
    return {"id": poll_id, **polls[poll_id]}

@app.put("/polls/{poll_id}")
def update_poll(poll_id: str, poll: PollCreate):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Poll not found")
    polls[poll_id] = {"title": poll.title, "options": poll.options}
    return {"id": poll_id, **polls[poll_id]}

@app.delete("/polls/{poll_id}")
def delete_poll(poll_id: str):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Poll not found")
    del polls[poll_id]
    del votes[poll_id]
    return {"message": "Deleted"}

@app.post("/polls/{poll_id}/vote")
def cast_vote(poll_id: str, vote: Vote):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Poll not found")
    if vote.option not in polls[poll_id]["options"]:
        raise HTTPException(status_code=400, detail="Invalid option")
    votes[poll_id][vote.voter_name] = vote.option
    return {"message": "Vote cast"}

@app.get("/polls/{poll_id}/results")
def get_results(poll_id: str):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Poll not found")
    results = {option: 0 for option in polls[poll_id]["options"]}
    for option in votes[poll_id].values():
        results[option] += 1
    return {"title": polls[poll_id]["title"], "results": results, "total_votes": len(votes[poll_id])}

@app.delete("/polls/{poll_id}/votes/{voter_name}")
def remove_vote(poll_id: str, voter_name: str):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Poll not found")
    if voter_name not in votes[poll_id]:
        raise HTTPException(status_code=404, detail="Vote not found")
    del votes[poll_id][voter_name]
    return {"message": "Vote removed"}

@app.post("/polls/{poll_id}/options")
def add_option(poll_id: str, option: str):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Poll not found")
    polls[poll_id]["options"].append(option)
    return {"message": "Option added"}

@app.delete("/polls/{poll_id}/options/{option}")
def delete_option(poll_id: str, option: str):
    if poll_id not in polls:
        raise HTTPException(status_code=404, detail="Poll not found")
    if option not in polls[poll_id]["options"]:
        raise HTTPException(status_code=404, detail="Option not found")
    polls[poll_id]["options"].remove(option)
    return {"message": "Option deleted"}